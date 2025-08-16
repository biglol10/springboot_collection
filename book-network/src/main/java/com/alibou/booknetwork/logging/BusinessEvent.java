package com.alibou.booknetwork.logging;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 비즈니스 이벤트 모델
 * 
 * 이 클래스는 도서 관리, 사용자 활동 등 비즈니스 핵심 이벤트를 구조화합니다.
 * 
 * 주요 필드:
 * - eventName: 이벤트 유형 (book_registration, book_borrow 등)
 * - userId: 이벤트를 발생시킨 사용자 ID
 * - entityType: 대상 엔티티 타입 (book, user, feedback 등)
 * - entityId: 대상 엔티티의 고유 ID
 * - action: 수행된 작업 (create, update, delete, borrow, return 등)
 * - status: 작업 결과 (success, failure, pending)
 * - duration: 작업 소요 시간 (밀리초)
 * - metadata: 추가 컨텍스트 정보
 * 
 * 사용 예시:
 * - 도서 등록: eventName=book_registration, action=create, entityType=book
 * - 도서 대여: eventName=book_borrow, action=borrow, entityType=book
 * - 피드백 작성: eventName=feedback_creation, action=create, entityType=feedback
 */
@Data
@Builder
public class BusinessEvent {
    
    /**
     * 이벤트 이름 (예: book_registration, book_borrow, user_login)
     */
    private String eventName;
    
    /**
     * 이벤트를 발생시킨 사용자 ID
     */
    private String userId;
    
    /**
     * 대상 엔티티 타입 (book, user, feedback, comment 등)
     */
    private String entityType;
    
    /**
     * 대상 엔티티의 고유 식별자
     */
    private String entityId;
    
    /**
     * 수행된 작업 (create, update, delete, borrow, return, search 등)
     */
    private String action;
    
    /**
     * 작업 결과 상태 (success, failure, pending, cancelled)
     */
    private String status;
    
    /**
     * 작업 소요 시간 (밀리초)
     */
    private Long duration;
    
    /**
     * 추가 메타데이터 (컨텍스트 정보, 비즈니스 데이터 등)
     */
    private Map<String, Object> metadata;
    
    // 자주 사용되는 이벤트 타입들을 상수로 정의
    public static class EventNames {
        public static final String BOOK_REGISTRATION = "book_registration";
        public static final String BOOK_UPDATE = "book_update";
        public static final String BOOK_DELETE = "book_delete";
        public static final String BOOK_BORROW = "book_borrow";
        public static final String BOOK_RETURN = "book_return";
        public static final String BOOK_SEARCH = "book_search";
        public static final String BOOK_RECOMMENDATION = "book_recommendation";
        
        public static final String USER_REGISTRATION = "user_registration";
        public static final String USER_PROFILE_UPDATE = "user_profile_update";
        public static final String USER_DEACTIVATION = "user_deactivation";
        
        public static final String FEEDBACK_CREATION = "feedback_creation";
        public static final String FEEDBACK_UPDATE = "feedback_update";
        public static final String FEEDBACK_DELETE = "feedback_delete";
        
        public static final String EMAIL_SENT = "email_sent";
        public static final String NOTIFICATION_SENT = "notification_sent";
    }
    
    // 엔티티 타입 상수들
    public static class EntityTypes {
        public static final String BOOK = "book";
        public static final String USER = "user";
        public static final String FEEDBACK = "feedback";
        public static final String COMMENT = "comment";
        public static final String EMAIL = "email";
        public static final String NOTIFICATION = "notification";
    }
    
    // 액션 타입 상수들
    public static class Actions {
        public static final String CREATE = "create";
        public static final String UPDATE = "update";
        public static final String DELETE = "delete";
        public static final String BORROW = "borrow";
        public static final String RETURN = "return";
        public static final String SEARCH = "search";
        public static final String VIEW = "view";
        public static final String DOWNLOAD = "download";
        public static final String UPLOAD = "upload";
        public static final String SEND = "send";
    }
    
    // 상태 타입 상수들
    public static class Status {
        public static final String SUCCESS = "success";
        public static final String FAILURE = "failure";
        public static final String PENDING = "pending";
        public static final String CANCELLED = "cancelled";
        public static final String TIMEOUT = "timeout";
    }
}