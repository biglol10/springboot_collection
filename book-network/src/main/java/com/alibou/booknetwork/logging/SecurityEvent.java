package com.alibou.booknetwork.logging;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 보안 이벤트 모델
 * 
 * 이 클래스는 인증, 인가, 보안 위협 등 보안 관련 이벤트를 구조화합니다.
 * 
 * 주요 필드:
 * - eventName: 보안 이벤트 유형 (user_login, permission_denied 등)
 * - userId: 관련 사용자 ID
 * - ipAddress: 요청 발생 IP 주소
 * - userAgent: 사용자 브라우저/클라이언트 정보
 * - action: 수행된 보안 액션 (authenticate, authorize 등)
 * - result: 보안 검증 결과 (success, failure, blocked)
 * - riskLevel: 위험도 레벨 (low, medium, high, critical)
 * - metadata: 추가 보안 컨텍스트 정보
 * 
 * 사용 예시:
 * - 로그인 시도: eventName=user_login, action=authenticate
 * - 권한 없는 접근: eventName=unauthorized_access, riskLevel=high
 * - 의심스러운 활동: eventName=suspicious_activity, riskLevel=critical
 */
@Data
@Builder
public class SecurityEvent {
    
    /**
     * 보안 이벤트 이름 (예: user_login, permission_denied, suspicious_activity)
     */
    private String eventName;
    
    /**
     * 관련 사용자 ID (인증되지 않은 경우 null 가능)
     */
    private String userId;
    
    /**
     * 요청 발생 IP 주소
     */
    private String ipAddress;
    
    /**
     * 사용자 에이전트 (브라우저, 앱 등의 클라이언트 정보)
     */
    private String userAgent;
    
    /**
     * 수행된 보안 액션 (authenticate, authorize, validate 등)
     */
    private String action;
    
    /**
     * 보안 검증 결과 (success, failure, blocked, suspicious)
     */
    private String result;
    
    /**
     * 위험도 레벨 (low, medium, high, critical)
     */
    private String riskLevel;
    
    /**
     * 추가 보안 메타데이터 (세션 정보, 디바이스 정보 등)
     */
    private Map<String, Object> metadata;
    
    // 자주 사용되는 보안 이벤트 타입들
    public static class EventNames {
        public static final String USER_LOGIN = "user_login";
        public static final String USER_LOGOUT = "user_logout";
        public static final String LOGIN_FAILURE = "login_failure";
        public static final String PASSWORD_CHANGE = "password_change";
        public static final String ACCOUNT_LOCKED = "account_locked";
        public static final String ACCOUNT_UNLOCKED = "account_unlocked";
        
        public static final String UNAUTHORIZED_ACCESS = "unauthorized_access";
        public static final String PERMISSION_DENIED = "permission_denied";
        public static final String PRIVILEGE_ESCALATION_ATTEMPT = "privilege_escalation_attempt";
        
        public static final String SUSPICIOUS_ACTIVITY = "suspicious_activity";
        public static final String BRUTE_FORCE_ATTEMPT = "brute_force_attempt";
        public static final String SQL_INJECTION_ATTEMPT = "sql_injection_attempt";
        public static final String XSS_ATTEMPT = "xss_attempt";
        
        public static final String TOKEN_ISSUED = "token_issued";
        public static final String TOKEN_EXPIRED = "token_expired";
        public static final String TOKEN_REVOKED = "token_revoked";
        public static final String TOKEN_VALIDATION_FAILED = "token_validation_failed";
        
        public static final String SESSION_CREATED = "session_created";
        public static final String SESSION_EXPIRED = "session_expired";
        public static final String SESSION_HIJACKING_ATTEMPT = "session_hijacking_attempt";
        
        public static final String DATA_BREACH_ATTEMPT = "data_breach_attempt";
        public static final String SENSITIVE_DATA_ACCESS = "sensitive_data_access";
        public static final String AUDIT_LOG_TAMPERING = "audit_log_tampering";
    }
    
    // 보안 액션 타입들
    public static class Actions {
        public static final String AUTHENTICATE = "authenticate";
        public static final String AUTHORIZE = "authorize";
        public static final String VALIDATE = "validate";
        public static final String ENCRYPT = "encrypt";
        public static final String DECRYPT = "decrypt";
        public static final String SIGN = "sign";
        public static final String VERIFY = "verify";
        public static final String BLOCK = "block";
        public static final String ALLOW = "allow";
        public static final String AUDIT = "audit";
    }
    
    // 결과 타입들
    public static class Results {
        public static final String SUCCESS = "success";
        public static final String FAILURE = "failure";
        public static final String BLOCKED = "blocked";
        public static final String SUSPICIOUS = "suspicious";
        public static final String TIMEOUT = "timeout";
        public static final String CANCELLED = "cancelled";
    }
    
    // 위험도 레벨들
    public static class RiskLevels {
        public static final String LOW = "low";
        public static final String MEDIUM = "medium";
        public static final String HIGH = "high";
        public static final String CRITICAL = "critical";
    }
}