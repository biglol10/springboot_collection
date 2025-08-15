package com.alibou.booknetwork.security.exception;

/**
 * 인증 관련 예외 클래스
 * 
 * 사용자 인증 과정에서 발생하는 모든 예외를 처리합니다.
 * 로그인, 권한 검증, 사용자 정보 조회 등의 과정에서 사용됩니다.
 */
public class AuthenticationException extends SecurityException {

    // 인증 관련 에러 코드
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_DISABLED = "USER_DISABLED";
    public static final String USER_LOCKED = "USER_LOCKED";
    public static final String CREDENTIALS_EXPIRED = "CREDENTIALS_EXPIRED";
    public static final String ACCOUNT_EXPIRED = "ACCOUNT_EXPIRED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String USER_ALREADY_AUTHENTICATED = "USER_ALREADY_AUTHENTICATED";
    public static final String MOCK_LOGIN_DISABLED = "MOCK_LOGIN_DISABLED";

    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public AuthenticationException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }

    public AuthenticationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public AuthenticationException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }

    // 편의 메서드들
    public static AuthenticationException userNotFound(String username) {
        return new AuthenticationException(USER_NOT_FOUND, 
            "User not found: %s".formatted(username), username);
    }

    public static AuthenticationException userDisabled(String username) {
        return new AuthenticationException(USER_DISABLED, 
            "User account is disabled: %s".formatted(username), username);
    }

    public static AuthenticationException userLocked(String username) {
        return new AuthenticationException(USER_LOCKED, 
            "User account is locked: %s".formatted(username), username);
    }

    public static AuthenticationException credentialsExpired(String username) {
        return new AuthenticationException(CREDENTIALS_EXPIRED, 
            "User credentials expired: %s".formatted(username), username);
    }

    public static AuthenticationException accountExpired(String username) {
        return new AuthenticationException(ACCOUNT_EXPIRED, 
            "User account expired: %s".formatted(username), username);
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException(INVALID_CREDENTIALS, "Invalid credentials provided");
    }

    public static AuthenticationException authenticationRequired() {
        return new AuthenticationException(AUTHENTICATION_REQUIRED, "Authentication is required");
    }

    public static AuthenticationException authenticationFailed(String reason) {
        return new AuthenticationException(AUTHENTICATION_FAILED, 
            "Authentication failed: %s".formatted(reason), reason);
    }

    public static AuthenticationException alreadyAuthenticated() {
        return new AuthenticationException(USER_ALREADY_AUTHENTICATED, "User is already authenticated");
    }

    public static AuthenticationException mockLoginDisabled() {
        return new AuthenticationException(MOCK_LOGIN_DISABLED, 
            "Mock login is disabled in this environment");
    }
}