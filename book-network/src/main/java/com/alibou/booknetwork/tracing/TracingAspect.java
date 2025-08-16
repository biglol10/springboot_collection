package com.alibou.booknetwork.tracing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 분산 추적 AOP 인터셉터
 * 
 * 이 클래스는 어노테이션 기반으로 메서드 실행을 자동으로 추적합니다.
 * 
 * 주요 기능:
 * 1. @Traced 어노테이션이 있는 메서드 자동 추적
 * 2. 메서드 파라미터 및 반환값 로깅
 * 3. 실행 시간 측정 및 성능 임계값 모니터링
 * 4. 예외 발생 시 자동 에러 추적
 * 5. 비즈니스 도메인별 커스텀 태그 추가
 * 
 * 왜 필요한가?
 * - 코드 수정 없이 추적 기능 추가
 * - 일관성 있는 추적 메타데이터 수집
 * - 개발 생산성 향상
 * - 운영 가시성 확보
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TracingAspect {

    private final TracingService tracingService;

    /**
     * @Traced 어노테이션이 있는 모든 메서드를 인터셉트합니다.
     */
    @Around("@annotation(traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        
        // 추적 스팬 이름 생성
        String spanName = traced.value().isEmpty() ? 
            className + "." + methodName : 
            traced.value();

        return tracingService.traceOperation(spanName, () -> {
            try {
                // 메서드 실행 전 컨텍스트 설정
                setMethodContext(joinPoint, traced);
                
                long startTime = System.currentTimeMillis();
                Object result = joinPoint.proceed();
                long duration = System.currentTimeMillis() - startTime;
                
                // 실행 후 메타데이터 추가
                setExecutionMetadata(duration, result, traced);
                
                // 성능 임계값 체크
                checkPerformanceThreshold(duration, traced, spanName);
                
                return result;
                
            } catch (Throwable e) {
                // 예외 정보 추가
                setErrorMetadata(e, traced);
                throw e;
            }
        });
    }

    /**
     * 서비스 레이어 메서드들을 자동으로 추적합니다.
     */
    @Around("execution(* com.alibou.booknetwork.*.service.*.*(..))")
    public Object traceServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String spanName = "service." + className + "." + methodName;

        return tracingService.traceOperation(spanName, () -> {
            try {
                // 서비스 레이어 특화 컨텍스트
                tracingService.addTagToCurrentSpan("layer", "service");
                tracingService.addTagToCurrentSpan("service.class", className);
                tracingService.addTagToCurrentSpan("service.method", methodName);
                
                return joinPoint.proceed();
                
            } catch (Throwable e) {
                tracingService.addTagToCurrentSpan("service.error", e.getMessage());
                throw e;
            }
        });
    }

    /**
     * 리포지토리 레이어 메서드들을 자동으로 추적합니다.
     */
    @Around("execution(* com.alibou.booknetwork.*.repository.*.*(..))")
    public Object traceRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String spanName = "repository." + className + "." + methodName;

        return tracingService.traceDatabaseOperation(
            methodName, 
            "Repository method: " + className + "." + methodName,
            () -> {
                try {
                    // 리포지토리 레이어 특화 컨텍스트
                    tracingService.addTagToCurrentSpan("layer", "repository");
                    tracingService.addTagToCurrentSpan("repository.class", className);
                    tracingService.addTagToCurrentSpan("repository.method", methodName);
                    
                    return joinPoint.proceed();
                    
                } catch (Throwable e) {
                    tracingService.addTagToCurrentSpan("repository.error", e.getMessage());
                    throw e;
                }
            }
        );
    }

    /**
     * 컨트롤러 레이어 메서드들을 자동으로 추적합니다.
     */
    @Around("execution(* com.alibou.booknetwork.*.controller.*.*(..))")
    public Object traceControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String spanName = "controller." + className + "." + methodName;

        return tracingService.traceOperation(spanName, () -> {
            try {
                // 컨트롤러 레이어 특화 컨텍스트
                tracingService.addTagToCurrentSpan("layer", "controller");
                tracingService.addTagToCurrentSpan("controller.class", className);
                tracingService.addTagToCurrentSpan("controller.method", methodName);
                
                // HTTP 요청 관련 정보 추가 (RequestContextHolder에서 추출)
                addHttpContextIfAvailable();
                
                return joinPoint.proceed();
                
            } catch (Throwable e) {
                tracingService.addTagToCurrentSpan("controller.error", e.getMessage());
                throw e;
            }
        });
    }

    /**
     * 비동기 메서드들을 추적합니다.
     */
    @Around("@annotation(org.springframework.scheduling.annotation.Async)")
    public Object traceAsyncMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String spanName = "async." + className + "." + methodName;

        // 비동기 메서드는 별도 스레드에서 실행되므로 새로운 트레이스 컨텍스트 생성
        tracingService.traceAsyncOperation(spanName, () -> {
            try {
                tracingService.addTagToCurrentSpan("execution.type", "async");
                tracingService.addTagToCurrentSpan("async.class", className);
                tracingService.addTagToCurrentSpan("async.method", methodName);
                
                joinPoint.proceed();
                
            } catch (Throwable e) {
                tracingService.addTagToCurrentSpan("async.error", e.getMessage());
                log.error("비동기 메서드 실행 중 오류: {}.{}", className, methodName, e);
            }
        });
        
        return null; // 비동기 메서드는 반환값이 없음
    }

    /**
     * 캐시 관련 메서드들을 추적합니다.
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable) || " +
            "@annotation(org.springframework.cache.annotation.CacheEvict) || " +
            "@annotation(org.springframework.cache.annotation.CachePut)")
    public Object traceCacheOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String cacheKey = extractCacheKey(joinPoint);
        
        return tracingService.traceCacheOperation(methodName, cacheKey, () -> {
            try {
                tracingService.addTagToCurrentSpan("cache.class", className);
                tracingService.addTagToCurrentSpan("cache.method", methodName);
                
                return joinPoint.proceed();
                
            } catch (Throwable e) {
                tracingService.addTagToCurrentSpan("cache.error", e.getMessage());
                throw e;
            }
        });
    }

    // 헬퍼 메서드들

    /**
     * 메서드 실행 컨텍스트를 설정합니다.
     */
    private void setMethodContext(ProceedingJoinPoint joinPoint, Traced traced) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        
        Map<String, String> tags = new HashMap<>();
        tags.put("method.class", className);
        tags.put("method.name", methodName);
        
        // 도메인 정보 추가
        if (!traced.domain().isEmpty()) {
            tags.put("business.domain", traced.domain());
        }
        
        // 연산 타입 추가
        if (!traced.operation().isEmpty()) {
            tags.put("business.operation", traced.operation());
        }
        
        // 파라미터 정보 추가 (민감하지 않은 경우에만)
        if (traced.includeParameters()) {
            addParameterInfo(joinPoint, tags);
        }
        
        tracingService.addTagsToCurrentSpan(tags);
    }

    /**
     * 실행 후 메타데이터를 설정합니다.
     */
    private void setExecutionMetadata(long duration, Object result, Traced traced) {
        Map<String, String> tags = new HashMap<>();
        tags.put("execution.duration_ms", String.valueOf(duration));
        
        // 반환값 정보 추가 (민감하지 않은 경우에만)
        if (traced.includeResult() && result != null) {
            tags.put("result.type", result.getClass().getSimpleName());
            
            // 컬렉션인 경우 크기 정보 추가
            if (result instanceof java.util.Collection) {
                tags.put("result.size", String.valueOf(((java.util.Collection<?>) result).size()));
            }
        }
        
        tracingService.addTagsToCurrentSpan(tags);
    }

    /**
     * 에러 메타데이터를 설정합니다.
     */
    private void setErrorMetadata(Throwable e, Traced traced) {
        Map<String, String> tags = new HashMap<>();
        tags.put("error.occurred", "true");
        tags.put("error.class", e.getClass().getSimpleName());
        tags.put("error.message", e.getMessage());
        
        // 스택 트레이스 포함 여부
        if (traced.includeStackTrace()) {
            tags.put("error.stack_trace", getStackTraceString(e));
        }
        
        tracingService.addTagsToCurrentSpan(tags);
    }

    /**
     * 성능 임계값을 체크합니다.
     */
    private void checkPerformanceThreshold(long duration, Traced traced, String spanName) {
        if (traced.performanceThresholdMs() > 0 && duration > traced.performanceThresholdMs()) {
            tracingService.addTagToCurrentSpan("performance.warning", "slow_execution");
            tracingService.addTagToCurrentSpan("performance.threshold_ms", String.valueOf(traced.performanceThresholdMs()));
            
            log.warn("느린 메서드 실행 감지: {} ({}ms > {}ms)", 
                    spanName, duration, traced.performanceThresholdMs());
        }
    }

    /**
     * HTTP 컨텍스트 정보를 추가합니다.
     */
    private void addHttpContextIfAvailable() {
        try {
            org.springframework.web.context.request.RequestAttributes requestAttributes = 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                javax.servlet.http.HttpServletRequest request = 
                    ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
                
                tracingService.addTagToCurrentSpan("http.method", request.getMethod());
                tracingService.addTagToCurrentSpan("http.uri", request.getRequestURI());
                tracingService.addTagToCurrentSpan("http.remote_addr", request.getRemoteAddr());
            }
        } catch (Exception e) {
            // HTTP 컨텍스트를 사용할 수 없는 경우 무시
            log.debug("HTTP 컨텍스트 정보를 가져올 수 없음: {}", e.getMessage());
        }
    }

    /**
     * 파라미터 정보를 안전하게 추가합니다.
     */
    private void addParameterInfo(ProceedingJoinPoint joinPoint, Map<String, String> tags) {
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            tags.put("method.parameter_count", String.valueOf(args.length));
            
            // 첫 번째 파라미터의 타입 정보만 추가 (값은 민감할 수 있으므로 제외)
            if (args[0] != null) {
                tags.put("method.first_parameter_type", args[0].getClass().getSimpleName());
            }
        }
    }

    /**
     * 캐시 키를 추출합니다.
     */
    private String extractCacheKey(ProceedingJoinPoint joinPoint) {
        // 실제 캐시 키 추출 로직 구현
        // 여기서는 메서드 이름과 첫 번째 파라미터를 조합
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        
        if (args != null && args.length > 0 && args[0] != null) {
            return methodName + ":" + args[0].toString();
        }
        
        return methodName;
    }

    /**
     * 스택 트레이스를 문자열로 변환합니다.
     */
    private String getStackTraceString(Throwable e) {
        return Arrays.stream(e.getStackTrace())
                .limit(5) // 처음 5개 스택만 포함
                .map(StackTraceElement::toString)
                .reduce("", (acc, line) -> acc + line + "\n");
    }
}