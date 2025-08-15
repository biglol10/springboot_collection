package com.alibou.booknetwork.security.exception;

/**
 * JWT 토큰 관련 예외 클래스
 * 
 * 토큰 생성, 검증, 파싱 중 발생하는 모든 예외를 처리합니다.
 * 세분화된 에러 코드를 통해 정확한 예외 상황을 전달합니다.
 */
public class TokenException extends SecurityException {

    // 토큰 관련 에러 코드
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String TOKEN_INVALID = "TOKEN_INVALID";
    public static final String TOKEN_MALFORMED = "TOKEN_MALFORMED";
    public static final String TOKEN_SIGNATURE_INVALID = "TOKEN_SIGNATURE_INVALID";
    public static final String TOKEN_UNSUPPORTED = "TOKEN_UNSUPPORTED";
    public static final String TOKEN_MISSING = "TOKEN_MISSING";
    public static final String TOKEN_BLACKLISTED = "TOKEN_BLACKLISTED";
    public static final String TOKEN_WRONG_TYPE = "TOKEN_WRONG_TYPE";
    public static final String TOKEN_IP_MISMATCH = "TOKEN_IP_MISMATCH";
    public static final String TOKEN_USER_AGENT_MISMATCH = "TOKEN_USER_AGENT_MISMATCH";
    public static final String TOKEN_ISSUER_INVALID = "TOKEN_ISSUER_INVALID";
    public static final String TOKEN_AUDIENCE_INVALID = "TOKEN_AUDIENCE_INVALID";
    public static final String TOKEN_NOT_YET_VALID = "TOKEN_NOT_YET_VALID";

    public TokenException(String errorCode, String message) {
        super(errorCode, message);
    }

    public TokenException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }

    public TokenException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public TokenException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }

    // 편의 메서드들
    public static TokenException expired(String message) {
        return new TokenException(TOKEN_EXPIRED, message);
    }

    public static TokenException invalid(String message) {
        return new TokenException(TOKEN_INVALID, message);
    }

    public static TokenException malformed(String message) {
        return new TokenException(TOKEN_MALFORMED, message);
    }

    public static TokenException signatureInvalid(String message) {
        return new TokenException(TOKEN_SIGNATURE_INVALID, message);
    }

    public static TokenException unsupported(String message) {
        return new TokenException(TOKEN_UNSUPPORTED, message);
    }

    public static TokenException missing(String message) {
        return new TokenException(TOKEN_MISSING, message);
    }

    public static TokenException blacklisted(String tokenId) {
        return new TokenException(TOKEN_BLACKLISTED, "Token is blacklisted", tokenId);
    }

    public static TokenException wrongType(String expected, String actual) {
        return new TokenException(TOKEN_WRONG_TYPE, 
            "Wrong token type. Expected: %s, Found: %s".formatted(expected, actual), 
            expected, actual);
    }

    public static TokenException ipMismatch(String tokenIp, String requestIp) {
        return new TokenException(TOKEN_IP_MISMATCH, 
            "IP address mismatch. Token IP: %s, Request IP: %s".formatted(tokenIp, requestIp), 
            tokenIp, requestIp);
    }

    public static TokenException issuerInvalid(String expected, String actual) {
        return new TokenException(TOKEN_ISSUER_INVALID, 
            "Invalid token issuer. Expected: %s, Found: %s".formatted(expected, actual), 
            expected, actual);
    }

    public static TokenException audienceInvalid(String expected, String actual) {
        return new TokenException(TOKEN_AUDIENCE_INVALID, 
            "Invalid token audience. Expected: %s, Found: %s".formatted(expected, actual), 
            expected, actual);
    }

    public static TokenException notYetValid(String message) {
        return new TokenException(TOKEN_NOT_YET_VALID, message);
    }
}