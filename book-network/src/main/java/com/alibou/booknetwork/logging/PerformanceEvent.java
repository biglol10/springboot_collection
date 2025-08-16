package com.alibou.booknetwork.logging;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 성능 이벤트 모델
 * 
 * 이 클래스는 시스템 성능, 응답 시간, 자원 사용량 등 성능 관련 이벤트를 구조화합니다.
 * 
 * 주요 필드:
 * - eventName: 성능 이벤트 유형 (api_response_time, db_query_time 등)
 * - operation: 수행된 작업 (GET /api/books, database_query 등)
 * - durationMs: 작업 소요 시간 (밀리초)
 * - cpuUsage: CPU 사용률 (%)
 * - memoryUsage: 메모리 사용률 (%)
 * - threadCount: 활성 스레드 수
 * - metadata: 추가 성능 컨텍스트 정보
 * 
 * 사용 예시:
 * - API 응답 시간: eventName=api_response_time, operation=GET /api/books
 * - 데이터베이스 쿼리: eventName=db_query_time, operation=SELECT books
 * - 캐시 성능: eventName=cache_performance, metadata={hit_ratio: 0.95}
 */
@Data
@Builder
public class PerformanceEvent {
    
    /**
     * 성능 이벤트 이름 (예: api_response_time, db_query_time, cache_performance)
     */
    private String eventName;
    
    /**
     * 수행된 작업 (예: GET /api/books, database_query, cache_lookup)
     */
    private String operation;
    
    /**
     * 작업 소요 시간 (밀리초)
     */
    private Long durationMs;
    
    /**
     * CPU 사용률 (0.0 ~ 100.0)
     */
    private Double cpuUsage;
    
    /**
     * 메모리 사용률 (0.0 ~ 100.0)
     */
    private Double memoryUsage;
    
    /**
     * 활성 스레드 수
     */
    private Integer threadCount;
    
    /**
     * 디스크 I/O 사용량 (바이트/초)
     */
    private Long diskIoUsage;
    
    /**
     * 네트워크 I/O 사용량 (바이트/초)
     */
    private Long networkIoUsage;
    
    /**
     * 처리된 요청 수 (처리량 측정용)
     */
    private Long requestCount;
    
    /**
     * 에러 발생 수
     */
    private Long errorCount;
    
    /**
     * 추가 성능 메타데이터 (쿼리 복잡도, 캐시 히트율 등)
     */
    private Map<String, Object> metadata;
    
    // 자주 사용되는 성능 이벤트 타입들
    public static class EventNames {
        public static final String API_RESPONSE_TIME = "api_response_time";
        public static final String DB_QUERY_TIME = "db_query_time";
        public static final String CACHE_PERFORMANCE = "cache_performance";
        public static final String EMAIL_SENDING_TIME = "email_sending_time";
        public static final String FILE_UPLOAD_TIME = "file_upload_time";
        public static final String SEARCH_PERFORMANCE = "search_performance";
        public static final String RECOMMENDATION_TIME = "recommendation_time";
        
        public static final String SYSTEM_RESOURCE_USAGE = "system_resource_usage";
        public static final String JVM_GARBAGE_COLLECTION = "jvm_garbage_collection";
        public static final String THREAD_POOL_PERFORMANCE = "thread_pool_performance";
        public static final String CONNECTION_POOL_PERFORMANCE = "connection_pool_performance";
        
        public static final String BATCH_JOB_PERFORMANCE = "batch_job_performance";
        public static final String ASYNC_TASK_PERFORMANCE = "async_task_performance";
        public static final String WEBHOOK_DELIVERY_TIME = "webhook_delivery_time";
        
        public static final String PAGE_LOAD_TIME = "page_load_time";
        public static final String COMPONENT_RENDER_TIME = "component_render_time";
        public static final String WEBSOCKET_MESSAGE_TIME = "websocket_message_time";
    }
    
    // 작업 타입들
    public static class Operations {
        // HTTP API 작업들
        public static final String GET_BOOKS = "GET /api/books";
        public static final String POST_BOOK = "POST /api/books";
        public static final String PUT_BOOK = "PUT /api/books/{id}";
        public static final String DELETE_BOOK = "DELETE /api/books/{id}";
        public static final String SEARCH_BOOKS = "GET /api/books/search";
        
        // 데이터베이스 작업들
        public static final String DB_SELECT = "database_select";
        public static final String DB_INSERT = "database_insert";
        public static final String DB_UPDATE = "database_update";
        public static final String DB_DELETE = "database_delete";
        public static final String DB_BATCH_INSERT = "database_batch_insert";
        
        // 캐시 작업들
        public static final String CACHE_GET = "cache_get";
        public static final String CACHE_PUT = "cache_put";
        public static final String CACHE_EVICT = "cache_evict";
        public static final String CACHE_CLEAR = "cache_clear";
        
        // 외부 서비스 호출들
        public static final String EMAIL_SEND = "email_send";
        public static final String SMS_SEND = "sms_send";
        public static final String PAYMENT_PROCESS = "payment_process";
        public static final String FILE_STORAGE_UPLOAD = "file_storage_upload";
        
        // 비즈니스 로직 작업들
        public static final String BOOK_RECOMMENDATION = "book_recommendation";
        public static final String USER_ANALYTICS = "user_analytics";
        public static final String REPORT_GENERATION = "report_generation";
        public static final String DATA_EXPORT = "data_export";
    }
    
    /**
     * 성능 레벨 분류를 위한 유틸리티 메서드
     */
    public String getPerformanceLevel() {
        if (durationMs == null) {
            return "unknown";
        }
        
        if (durationMs < 100) {
            return "excellent";  // 100ms 미만
        } else if (durationMs < 500) {
            return "good";       // 100-500ms
        } else if (durationMs < 1000) {
            return "fair";       // 500ms-1s
        } else if (durationMs < 3000) {
            return "poor";       // 1-3s
        } else {
            return "critical";   // 3s 이상
        }
    }
    
    /**
     * CPU 사용률 레벨 분류
     */
    public String getCpuUsageLevel() {
        if (cpuUsage == null) {
            return "unknown";
        }
        
        if (cpuUsage < 50.0) {
            return "low";
        } else if (cpuUsage < 70.0) {
            return "medium";
        } else if (cpuUsage < 90.0) {
            return "high";
        } else {
            return "critical";
        }
    }
    
    /**
     * 메모리 사용률 레벨 분류
     */
    public String getMemoryUsageLevel() {
        if (memoryUsage == null) {
            return "unknown";
        }
        
        if (memoryUsage < 60.0) {
            return "low";
        } else if (memoryUsage < 80.0) {
            return "medium";
        } else if (memoryUsage < 95.0) {
            return "high";
        } else {
            return "critical";
        }
    }
}