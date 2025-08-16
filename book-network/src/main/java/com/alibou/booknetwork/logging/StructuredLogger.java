package com.alibou.booknetwork.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 구조화된 로깅 유틸리티
 * 
 * 이 클래스는 일관성 있는 구조화된 로그를 생성하여 ELK Stack에서 효율적으로 검색하고 분석할 수 있도록 합니다.
 * 
 * 주요 기능:
 * 1. 비즈니스 이벤트 로깅 (도서 등록, 대여, 반납 등)
 * 2. 보안 이벤트 로깅 (로그인, 권한 변경 등)
 * 3. 성능 이벤트 로깅 (응답 시간, 처리량 등)
 * 4. 에러 이벤트 로깅 (예외, 장애 등)
 * 
 * 왜 필요한가?
 * - 로그 데이터의 일관성 보장
 * - 효율적인 로그 검색 및 분석
 * - 자동화된 모니터링 및 알림
 * - 비즈니스 인사이트 도출
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StructuredLogger {

    private static final Logger BUSINESS_LOGGER = LoggerFactory.getLogger("BUSINESS_EVENTS");
    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("SECURITY_EVENTS");
    private static final Logger PERFORMANCE_LOGGER = LoggerFactory.getLogger("PERFORMANCE_EVENTS");
    
    private final ObjectMapper objectMapper;

    /**
     * 비즈니스 이벤트 로깅
     * 도서 관리, 사용자 활동 등 핵심 비즈니스 로직 실행을 추적합니다.
     */
    public void logBusinessEvent(BusinessEvent event) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", Instant.now().toString());
            logData.put("event_type", "business");
            logData.put("event_name", event.getEventName());
            logData.put("user_id", event.getUserId());
            logData.put("entity_type", event.getEntityType());
            logData.put("entity_id", event.getEntityId());
            logData.put("action", event.getAction());
            logData.put("status", event.getStatus());
            logData.put("correlation_id", getCurrentCorrelationId());
            
            if (event.getMetadata() != null) {
                logData.put("metadata", event.getMetadata());
            }
            
            if (event.getDuration() != null) {
                logData.put("duration_ms", event.getDuration());
            }

            String jsonLog = objectMapper.writeValueAsString(logData);
            BUSINESS_LOGGER.info(jsonLog);
            
        } catch (Exception e) {
            log.error("비즈니스 이벤트 로깅 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 보안 이벤트 로깅
     * 인증, 인가, 보안 위협 등을 추적합니다.
     */
    public void logSecurityEvent(SecurityEvent event) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", Instant.now().toString());
            logData.put("event_type", "security");
            logData.put("event_name", event.getEventName());
            logData.put("user_id", event.getUserId());
            logData.put("ip_address", event.getIpAddress());
            logData.put("user_agent", event.getUserAgent());
            logData.put("action", event.getAction());
            logData.put("result", event.getResult());
            logData.put("risk_level", event.getRiskLevel());
            logData.put("correlation_id", getCurrentCorrelationId());
            
            if (event.getMetadata() != null) {
                logData.put("metadata", event.getMetadata());
            }

            String jsonLog = objectMapper.writeValueAsString(logData);
            SECURITY_LOGGER.info(jsonLog);
            
        } catch (Exception e) {
            log.error("보안 이벤트 로깅 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 성능 이벤트 로깅
     * 응답 시간, 처리량, 자원 사용량 등을 추적합니다.
     */
    public void logPerformanceEvent(PerformanceEvent event) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", Instant.now().toString());
            logData.put("event_type", "performance");
            logData.put("event_name", event.getEventName());
            logData.put("operation", event.getOperation());
            logData.put("duration_ms", event.getDurationMs());
            logData.put("cpu_usage", event.getCpuUsage());
            logData.put("memory_usage", event.getMemoryUsage());
            logData.put("thread_count", event.getThreadCount());
            logData.put("correlation_id", getCurrentCorrelationId());
            
            if (event.getMetadata() != null) {
                logData.put("metadata", event.getMetadata());
            }

            String jsonLog = objectMapper.writeValueAsString(logData);
            PERFORMANCE_LOGGER.info(jsonLog);
            
        } catch (Exception e) {
            log.error("성능 이벤트 로깅 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 에러 이벤트 로깅
     * 예외, 장애, 시스템 오류 등을 추적합니다.
     */
    public void logErrorEvent(String eventName, String errorMessage, Throwable throwable, 
                             String userId, Map<String, Object> context) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", Instant.now().toString());
            logData.put("event_type", "error");
            logData.put("event_name", eventName);
            logData.put("error_message", errorMessage);
            logData.put("user_id", userId);
            logData.put("correlation_id", getCurrentCorrelationId());
            
            if (throwable != null) {
                logData.put("exception_class", throwable.getClass().getSimpleName());
                logData.put("stack_trace", getStackTrace(throwable));
            }
            
            if (context != null) {
                logData.put("context", context);
            }

            String jsonLog = objectMapper.writeValueAsString(logData);
            log.error(jsonLog, throwable);
            
        } catch (Exception e) {
            log.error("에러 이벤트 로깅 실패: {} - 원본 에러: {}", e.getMessage(), errorMessage, e);
        }
    }

    /**
     * 책 등록 이벤트 로깅
     */
    public void logBookRegistration(String userId, String bookId, String title, boolean success) {
        BusinessEvent event = BusinessEvent.builder()
                .eventName("book_registration")
                .userId(userId)
                .entityType("book")
                .entityId(bookId)
                .action("create")
                .status(success ? "success" : "failure")
                .metadata(Map.of("title", title))
                .build();
        
        logBusinessEvent(event);
    }

    /**
     * 책 대여 이벤트 로깅
     */
    public void logBookBorrow(String userId, String bookId, String title, boolean success, Long durationMs) {
        BusinessEvent event = BusinessEvent.builder()
                .eventName("book_borrow")
                .userId(userId)
                .entityType("book")
                .entityId(bookId)
                .action("borrow")
                .status(success ? "success" : "failure")
                .duration(durationMs)
                .metadata(Map.of("title", title))
                .build();
        
        logBusinessEvent(event);
    }

    /**
     * 사용자 로그인 이벤트 로깅
     */
    public void logUserLogin(String userId, String ipAddress, String userAgent, boolean success) {
        SecurityEvent event = SecurityEvent.builder()
                .eventName("user_login")
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .action("authenticate")
                .result(success ? "success" : "failure")
                .riskLevel(success ? "low" : "medium")
                .build();
        
        logSecurityEvent(event);
    }

    /**
     * API 응답 시간 로깅
     */
    public void logApiPerformance(String endpoint, String method, long durationMs, int statusCode) {
        PerformanceEvent event = PerformanceEvent.builder()
                .eventName("api_response_time")
                .operation(method + " " + endpoint)
                .durationMs(durationMs)
                .metadata(Map.of(
                    "status_code", statusCode,
                    "endpoint", endpoint,
                    "method", method
                ))
                .build();
        
        logPerformanceEvent(event);
    }

    /**
     * 상관관계 ID 설정 및 관리
     */
    public void setCorrelationId(String correlationId) {
        MDC.put("correlation_id", correlationId);
    }

    public String getCurrentCorrelationId() {
        String correlationId = MDC.get("correlation_id");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            setCorrelationId(correlationId);
        }
        return correlationId;
    }

    public void clearCorrelationId() {
        MDC.remove("correlation_id");
    }

    /**
     * 사용자 컨텍스트 설정
     */
    public void setUserContext(String userId, String sessionId) {
        MDC.put("user_id", userId);
        MDC.put("session_id", sessionId);
    }

    public void clearUserContext() {
        MDC.remove("user_id");
        MDC.remove("session_id");
    }

    /**
     * 요청 컨텍스트 설정
     */
    public void setRequestContext(String method, String uri, String remoteAddr) {
        MDC.put("http_method", method);
        MDC.put("request_uri", uri);
        MDC.put("remote_addr", remoteAddr);
    }

    public void clearRequestContext() {
        MDC.remove("http_method");
        MDC.remove("request_uri");
        MDC.remove("remote_addr");
    }

    /**
     * 전체 MDC 컨텍스트 정리
     */
    public void clearAllContext() {
        MDC.clear();
    }

    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}