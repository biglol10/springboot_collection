package com.alibou.booknetwork.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 분산 추적 서비스
 * 
 * 이 클래스는 마이크로서비스 간의 요청 흐름을 추적하고 성능을 모니터링합니다.
 * 
 * 주요 기능:
 * 1. 커스텀 스팬 생성 및 관리
 * 2. 비즈니스 로직별 세부 추적
 * 3. 외부 서비스 호출 추적
 * 4. 에러 및 예외 상황 추적
 * 5. 성능 메트릭 수집
 * 
 * 왜 필요한가?
 * - 마이크로서비스 간 의존성 가시화
 * - 성능 병목 지점 식별
 * - 장애 원인 추적 및 디버깅
 * - 서비스 간 호출 패턴 분석
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TracingService {

    private final Tracer tracer;

    /**
     * 비즈니스 연산에 대한 커스텀 스팬을 생성하고 실행합니다.
     */
    public <T> T traceOperation(String operationName, Supplier<T> operation) {
        Span span = tracer.nextSpan()
                .name(operationName)
                .tag("component", "business-logic")
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            T result = operation.get();
            span.tag("operation.status", "success");
            return result;
        } catch (Exception e) {
            span.tag("operation.status", "error");
            span.tag("error.message", e.getMessage());
            span.tag("error.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * 비동기 작업을 추적합니다.
     */
    public void traceAsyncOperation(String operationName, Runnable operation) {
        Span span = tracer.nextSpan()
                .name(operationName)
                .tag("component", "async-task")
                .tag("async", "true")
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            operation.run();
            span.tag("operation.status", "success");
        } catch (Exception e) {
            span.tag("operation.status", "error");
            span.tag("error.message", e.getMessage());
            span.tag("error.class", e.getClass().getSimpleName());
            log.error("비동기 작업 중 오류 발생: {}", operationName, e);
        } finally {
            span.end();
        }
    }

    /**
     * 데이터베이스 연산을 추적합니다.
     */
    public <T> T traceDatabaseOperation(String operationName, String query, Supplier<T> operation) {
        Span span = tracer.nextSpan()
                .name("db." + operationName)
                .tag("component", "database")
                .tag("db.operation", operationName)
                .tag("db.query", sanitizeQuery(query))
                .tag("db.type", "postgresql")
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            long startTime = System.currentTimeMillis();
            T result = operation.get();
            long duration = System.currentTimeMillis() - startTime;
            
            span.tag("db.duration", String.valueOf(duration));
            span.tag("operation.status", "success");
            
            if (duration > 1000) {
                span.tag("performance.warning", "slow_query");
                log.warn("느린 데이터베이스 쿼리 감지: {} ({}ms)", operationName, duration);
            }
            
            return result;
        } catch (Exception e) {
            span.tag("operation.status", "error");
            span.tag("error.message", e.getMessage());
            span.tag("error.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * 캐시 연산을 추적합니다.
     */
    public <T> T traceCacheOperation(String operationName, String cacheKey, Supplier<T> operation) {
        Span span = tracer.nextSpan()
                .name("cache." + operationName)
                .tag("component", "cache")
                .tag("cache.operation", operationName)
                .tag("cache.key", cacheKey)
                .tag("cache.type", "redis")
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            long startTime = System.currentTimeMillis();
            T result = operation.get();
            long duration = System.currentTimeMillis() - startTime;
            
            span.tag("cache.duration", String.valueOf(duration));
            span.tag("operation.status", "success");
            
            // 캐시 결과에 따른 태그 추가
            if (result != null) {
                span.tag("cache.result", "hit");
            } else {
                span.tag("cache.result", "miss");
            }
            
            return result;
        } catch (Exception e) {
            span.tag("operation.status", "error");
            span.tag("cache.result", "error");
            span.tag("error.message", e.getMessage());
            span.tag("error.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * 외부 서비스 호출을 추적합니다.
     */
    public <T> T traceExternalServiceCall(String serviceName, String endpoint, Supplier<T> operation) {
        Span span = tracer.nextSpan()
                .name("http.client." + serviceName)
                .tag("component", "http-client")
                .tag("http.service", serviceName)
                .tag("http.endpoint", endpoint)
                .tag("span.kind", "client")
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            long startTime = System.currentTimeMillis();
            T result = operation.get();
            long duration = System.currentTimeMillis() - startTime;
            
            span.tag("http.duration", String.valueOf(duration));
            span.tag("operation.status", "success");
            span.tag("http.status_code", "200");
            
            if (duration > 5000) {
                span.tag("performance.warning", "slow_external_call");
                log.warn("느린 외부 서비스 호출 감지: {} {} ({}ms)", serviceName, endpoint, duration);
            }
            
            return result;
        } catch (Exception e) {
            span.tag("operation.status", "error");
            span.tag("http.status_code", "error");
            span.tag("error.message", e.getMessage());
            span.tag("error.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * 이메일 발송을 추적합니다.
     */
    public void traceEmailSending(String recipient, String subject, Runnable emailOperation) {
        Span span = tracer.nextSpan()
                .name("email.send")
                .tag("component", "email-service")
                .tag("email.recipient", maskEmail(recipient))
                .tag("email.subject", subject)
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            long startTime = System.currentTimeMillis();
            emailOperation.run();
            long duration = System.currentTimeMillis() - startTime;
            
            span.tag("email.duration", String.valueOf(duration));
            span.tag("operation.status", "success");
        } catch (Exception e) {
            span.tag("operation.status", "error");
            span.tag("error.message", e.getMessage());
            span.tag("error.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * 비즈니스 이벤트별 상세 추적
     */
    public void traceBookOperation(String operation, String bookId, String userId, Runnable businessOperation) {
        Span span = tracer.nextSpan()
                .name("book." + operation)
                .tag("component", "book-service")
                .tag("business.operation", operation)
                .tag("book.id", bookId)
                .tag("user.id", userId)
                .tag("domain", "library-management")
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            businessOperation.run();
            span.tag("operation.status", "success");
        } catch (Exception e) {
            span.tag("operation.status", "error");
            span.tag("error.message", e.getMessage());
            span.tag("error.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * 사용자 인증 관련 추적
     */
    public <T> T traceAuthenticationOperation(String operation, String userId, Supplier<T> authOperation) {
        Span span = tracer.nextSpan()
                .name("auth." + operation)
                .tag("component", "authentication-service")
                .tag("auth.operation", operation)
                .tag("user.id", userId)
                .tag("security.event", "true")
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            T result = authOperation.get();
            span.tag("auth.result", "success");
            span.tag("operation.status", "success");
            return result;
        } catch (Exception e) {
            span.tag("auth.result", "failure");
            span.tag("operation.status", "error");
            span.tag("error.message", e.getMessage());
            span.tag("error.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * 추천 시스템 추적
     */
    public <T> T traceRecommendationOperation(String algorithm, String userId, Supplier<T> recommendationOperation) {
        Span span = tracer.nextSpan()
                .name("recommendation.generate")
                .tag("component", "recommendation-engine")
                .tag("recommendation.algorithm", algorithm)
                .tag("user.id", userId)
                .tag("ml.model", algorithm)
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            long startTime = System.currentTimeMillis();
            T result = recommendationOperation.get();
            long duration = System.currentTimeMillis() - startTime;
            
            span.tag("recommendation.duration", String.valueOf(duration));
            span.tag("operation.status", "success");
            
            if (duration > 2000) {
                span.tag("performance.warning", "slow_recommendation");
                log.warn("느린 추천 시스템 응답: {} algorithm ({}ms)", algorithm, duration);
            }
            
            return result;
        } catch (Exception e) {
            span.tag("operation.status", "error");
            span.tag("error.message", e.getMessage());
            span.tag("error.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * 현재 트레이스 컨텍스트 정보 반환
     */
    public TraceContext getCurrentTraceContext() {
        Span currentSpan = tracer.currentSpan();
        return currentSpan != null ? currentSpan.context() : null;
    }

    /**
     * 현재 트레이스 ID 반환
     */
    public String getCurrentTraceId() {
        TraceContext context = getCurrentTraceContext();
        return context != null ? context.traceId() : null;
    }

    /**
     * 현재 스팬 ID 반환
     */
    public String getCurrentSpanId() {
        TraceContext context = getCurrentTraceContext();
        return context != null ? context.spanId() : null;
    }

    /**
     * 커스텀 태그 추가
     */
    public void addTagToCurrentSpan(String key, String value) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag(key, value);
        }
    }

    /**
     * 복수의 태그를 한번에 추가
     */
    public void addTagsToCurrentSpan(Map<String, String> tags) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null && tags != null) {
            tags.forEach(currentSpan::tag);
        }
    }

    /**
     * 이벤트 로그 추가
     */
    public void addEventToCurrentSpan(String eventName) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.event(eventName);
        }
    }

    // 유틸리티 메서드들

    /**
     * SQL 쿼리를 안전하게 마스킹합니다.
     */
    private String sanitizeQuery(String query) {
        if (query == null) return "unknown";
        
        // 쿼리를 간단하게 요약하고 민감한 데이터 제거
        return query.replaceAll("'[^']*'", "'***'")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    /**
     * 이메일 주소를 마스킹합니다.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "***@" + domain;
        }
        
        return localPart.substring(0, 2) + "***@" + domain;
    }
}