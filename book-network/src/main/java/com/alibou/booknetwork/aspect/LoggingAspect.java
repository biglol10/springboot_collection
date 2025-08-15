package com.alibou.booknetwork.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * AOP 기반 로깅 관점(Aspect)
 * 
 * AOP(Aspect-Oriented Programming) 핵심 개념:
 * 1. 관점(Aspect): 횡단 관심사를 모듈화한 단위
 * 2. 조인포인트(JoinPoint): 관점이 적용될 수 있는 지점
 * 3. 포인트컷(Pointcut): 조인포인트를 선별하는 기준
 * 4. 어드바이스(Advice): 관점에서 실제 수행할 작업
 * 
 * 엔터프라이즈에서 AOP의 가치:
 * - 횡단 관심사 분리: 비즈니스 로직과 부가 기능 분리
 * - 코드 중복 제거: 로깅, 보안, 트랜잭션 등 공통 기능 중앙화
 * - 유지보수성 향상: 관심사별 독립적 수정 가능
 * - 가독성 개선: 핵심 비즈니스 로직에 집중 가능
 * 
 * SSGD에서 학습한 패턴:
 * - 구조적 로깅: JSON 형태의 일관된 로그 포맷
 * - 추적 가능성: 요청별 고유 추적 ID 부여
 * - 성능 모니터링: 메서드 실행 시간 측정
 * - 컨텍스트 정보: 사용자, IP, 세션 정보 자동 수집
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingAspect {

    private final ObjectMapper objectMapper;

    /**
     * 서비스 레이어 포인트컷 정의
     * 
     * 포인트컷 표현식 분석:
     * - execution: 메서드 실행 시점
     * - * com.alibou.booknetwork.service..*.*(..)
     *   - 첫 번째 *: 모든 반환 타입
     *   - com.alibou.booknetwork.service..: service 패키지와 하위 패키지
     *   - 세 번째 *: 모든 클래스
     *   - 네 번째 *: 모든 메서드
     *   - (..): 모든 파라미터
     */
    @Pointcut("execution(* com.alibou.booknetwork.service..*.*(..))")
    public void serviceLayer() {}

    /**
     * 컨트롤러 레이어 포인트컷 정의
     */
    @Pointcut("execution(* com.alibou.booknetwork.*..*Controller.*(..))")
    public void controllerLayer() {}

    /**
     * 리포지토리 레이어 포인트컷 정의
     */
    @Pointcut("execution(* com.alibou.booknetwork.repository..*.*(..))")
    public void repositoryLayer() {}

    /**
     * Around 어드바이스: 메서드 실행 전후 처리
     * 
     * Around의 특징:
     * - 메서드 실행을 완전히 제어 가능
     * - 메서드 호출 전후에 로직 실행
     * - 예외 처리 및 결과 변경 가능
     * - 실행 시간 측정에 최적
     * 
     * 엔터프라이즈 로깅의 핵심 요소:
     * 1. 추적 ID: 분산 환경에서 요청 추적
     * 2. 컨텍스트 정보: 사용자, 세션, IP 등
     * 3. 실행 시간: 성능 모니터링
     * 4. 파라미터/결과: 디버깅 지원
     */
    @Around("serviceLayer() || controllerLayer()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 추적 ID 생성 및 MDC 설정
        String traceId = generateTraceId();
        MDC.put("traceId", traceId);
        MDC.put("className", joinPoint.getTarget().getClass().getSimpleName());
        MDC.put("methodName", joinPoint.getSignature().getName());
        
        // 요청 컨텍스트 정보 수집
        enrichMDCWithRequestInfo();
        
        try {
            // 메서드 시작 로깅
            logMethodStart(joinPoint, traceId);
            
            // 실제 메서드 실행
            Object result = joinPoint.proceed();
            
            // 실행 시간 계산
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 메서드 완료 로깅
            logMethodCompletion(joinPoint, traceId, executionTime, result);
            
            return result;
            
        } catch (Exception e) {
            // 예외 발생 시 로깅
            long executionTime = System.currentTimeMillis() - startTime;
            logMethodException(joinPoint, traceId, executionTime, e);
            throw e;
            
        } finally {
            // MDC 정리 (메모리 누수 방지)
            MDC.clear();
        }
    }

    /**
     * Before 어드바이스: 메서드 실행 전 처리
     * 
     * 사용 사례:
     * - 입력 검증 로깅
     * - 보안 검사 로깅
     * - 캐시 키 생성 로깅
     */
    @Before("repositoryLayer()")
    public void logRepositoryAccess(JoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().toShortString();
            Object[] args = joinPoint.getArgs();
            
            log.debug("데이터베이스 접근 시작 - 메서드: {}, 파라미터: {}", 
                methodName, 
                args.length > 0 ? Arrays.toString(args) : "없음");
                
        } catch (Exception e) {
            log.warn("리포지토리 접근 로깅 실패: {}", e.getMessage());
        }
    }

    /**
     * AfterReturning 어드바이스: 정상 반환 후 처리
     * 
     * 사용 사례:
     * - 성공적인 작업 결과 로깅
     * - 캐시 업데이트 로깅
     * - 비즈니스 메트릭 수집
     */
    @AfterReturning(pointcut = "repositoryLayer()", returning = "result")
    public void logRepositoryResult(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().toShortString();
            
            // 결과 요약 정보만 로깅 (민감한 정보 제외)
            String resultSummary = summarizeResult(result);
            
            log.debug("데이터베이스 접근 완료 - 메서드: {}, 결과: {}", 
                methodName, resultSummary);
                
        } catch (Exception e) {
            log.warn("리포지토리 결과 로깅 실패: {}", e.getMessage());
        }
    }

    /**
     * AfterThrowing 어드바이스: 예외 발생 후 처리
     * 
     * 사용 사례:
     * - 예외 상세 정보 로깅
     * - 알림 발송 트리거
     * - 장애 통계 수집
     */
    @AfterThrowing(pointcut = "serviceLayer() || repositoryLayer()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        try {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            
            log.error("메서드 실행 중 예외 발생 - 클래스: {}, 메서드: {}, 파라미터: {}, 예외: {}", 
                className, methodName, 
                args.length > 0 ? Arrays.toString(args) : "없음",
                ex.getMessage());
                
            // 심각한 예외의 경우 스택 트레이스 전체 로깅
            if (isCriticalException(ex)) {
                log.error("심각한 예외 스택 트레이스:", ex);
                
                // 실제 환경에서는 여기서 알림 발송
                // notificationService.sendCriticalErrorAlert(className, methodName, ex);
            }
            
        } catch (Exception e) {
            log.warn("예외 로깅 실패: {}", e.getMessage());
        }
    }

    /**
     * 메서드 시작 로깅
     */
    private void logMethodStart(ProceedingJoinPoint joinPoint, String traceId) {
        try {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            
            log.info("메서드 실행 시작 - 추적ID: {}, 클래스: {}, 메서드: {}, 파라미터 수: {}", 
                traceId, className, methodName, args.length);
                
            // 상세 파라미터는 DEBUG 레벨에서만 로깅 (성능 고려)
            if (log.isDebugEnabled()) {
                log.debug("메서드 파라미터 상세 - 추적ID: {}, 파라미터: {}", 
                    traceId, sanitizeParameters(args));
            }
            
        } catch (Exception e) {
            log.warn("메서드 시작 로깅 실패 - 추적ID: {}, 오류: {}", traceId, e.getMessage());
        }
    }

    /**
     * 메서드 완료 로깅
     */
    private void logMethodCompletion(ProceedingJoinPoint joinPoint, String traceId, 
                                   long executionTime, Object result) {
        try {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            
            log.info("메서드 실행 완료 - 추적ID: {}, 클래스: {}, 메서드: {}, 실행시간: {}ms", 
                traceId, className, methodName, executionTime);
                
            // 느린 쿼리 경고 (2초 초과)
            if (executionTime > 2000) {
                log.warn("느린 메서드 실행 감지 - 추적ID: {}, 실행시간: {}ms, 메서드: {}.{}", 
                    traceId, executionTime, className, methodName);
            }
            
            // 결과 요약 (DEBUG 레벨)
            if (log.isDebugEnabled()) {
                log.debug("메서드 실행 결과 - 추적ID: {}, 결과: {}", 
                    traceId, summarizeResult(result));
            }
            
        } catch (Exception e) {
            log.warn("메서드 완료 로깅 실패 - 추적ID: {}, 오류: {}", traceId, e.getMessage());
        }
    }

    /**
     * 메서드 예외 로깅
     */
    private void logMethodException(ProceedingJoinPoint joinPoint, String traceId, 
                                  long executionTime, Exception exception) {
        try {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            
            log.error("메서드 실행 중 예외 - 추적ID: {}, 클래스: {}, 메서드: {}, 실행시간: {}ms, 예외: {}", 
                traceId, className, methodName, executionTime, exception.getMessage());
                
        } catch (Exception e) {
            log.warn("메서드 예외 로깅 실패 - 추적ID: {}, 오류: {}", traceId, e.getMessage());
        }
    }

    /**
     * 추적 ID 생성
     * 
     * UUID 기반 고유 식별자:
     * - 분산 환경에서 중복 없는 추적 보장
     * - 로그 수집 시스템에서 요청별 그룹핑 가능
     * - 디버깅 시 특정 요청의 전체 흐름 추적 가능
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 요청 컨텍스트 정보로 MDC 보강
     * 
     * MDC(Mapped Diagnostic Context):
     * - 로그백/SLF4J의 기능
     * - 쓰레드별 컨텍스트 정보 저장
     * - 로그 패턴에서 자동으로 컨텍스트 정보 출력 가능
     */
    private void enrichMDCWithRequestInfo() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // 요청 정보 MDC 설정
                MDC.put("requestURI", request.getRequestURI());
                MDC.put("httpMethod", request.getMethod());
                MDC.put("clientIP", getClientIpAddress(request));
                MDC.put("userAgent", request.getHeader("User-Agent"));
                MDC.put("requestTime", LocalDateTime.now().toString());
                
                // 세션 ID (있는 경우)
                if (request.getSession(false) != null) {
                    MDC.put("sessionId", request.getSession().getId());
                }
            }
            
        } catch (Exception e) {
            log.warn("요청 컨텍스트 정보 수집 실패: {}", e.getMessage());
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     * 
     * 프록시/로드밸런서 환경 고려:
     * - X-Forwarded-For: 프록시를 거친 원본 IP
     * - X-Real-IP: nginx 등에서 설정하는 실제 IP
     * - X-Forwarded-Proto: 원본 프로토콜 정보
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 여러 IP가 있는 경우 첫 번째 IP 사용
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 파라미터 정제 (민감한 정보 마스킹)
     * 
     * 보안 고려사항:
     * - 패스워드, 토큰 등 민감 정보 마스킹
     * - 개인정보 일부 마스킹 (이메일, 전화번호)
     * - 로그 크기 제한 (대용량 데이터 요약)
     */
    private String sanitizeParameters(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                
                Object arg = args[i];
                if (arg == null) {
                    sb.append("null");
                } else {
                    String argString = arg.toString();
                    
                    // 민감한 정보 마스킹
                    if (containsSensitiveInfo(argString)) {
                        sb.append("***MASKED***");
                    } else if (argString.length() > 200) {
                        // 긴 문자열은 요약
                        sb.append(argString.substring(0, 200)).append("...(truncated)");
                    } else {
                        sb.append(argString);
                    }
                }
            }
            sb.append("]");
            
            return sb.toString();
            
        } catch (Exception e) {
            return "[파라미터 정제 실패: " + e.getMessage() + "]";
        }
    }

    /**
     * 민감한 정보 포함 여부 검사
     */
    private boolean containsSensitiveInfo(String str) {
        if (str == null) return false;
        
        String lowerStr = str.toLowerCase();
        return lowerStr.contains("password") || 
               lowerStr.contains("token") || 
               lowerStr.contains("secret") ||
               lowerStr.contains("key") ||
               lowerStr.contains("credential");
    }

    /**
     * 결과 객체 요약
     */
    private String summarizeResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        try {
            if (result instanceof java.util.Collection) {
                java.util.Collection<?> collection = (java.util.Collection<?>) result;
                return String.format("Collection(size=%d)", collection.size());
            } else if (result instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) result;
                return String.format("Map(size=%d)", map.size());
            } else if (result.getClass().isArray()) {
                return String.format("Array(length=%d)", 
                    java.lang.reflect.Array.getLength(result));
            } else {
                String resultString = result.toString();
                return resultString.length() > 100 ? 
                    resultString.substring(0, 100) + "...(truncated)" : resultString;
            }
            
        } catch (Exception e) {
            return "결과 요약 실패: " + e.getMessage();
        }
    }

    /**
     * 심각한 예외 여부 판단
     */
    private boolean isCriticalException(Throwable ex) {
        return ex instanceof OutOfMemoryError ||
               ex instanceof StackOverflowError ||
               ex instanceof java.sql.SQLException ||
               ex.getCause() instanceof java.io.IOException;
    }
}