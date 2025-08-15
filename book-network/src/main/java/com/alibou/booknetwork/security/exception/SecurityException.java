package com.alibou.booknetwork.security.exception;

/**
 * 보안 관련 기본 예외 클래스
 * 
 * Book Network의 모든 보안 관련 예외의 부모 클래스입니다.
 * SSGD 스타일의 엔터프라이즈급 예외 처리를 위한 기반을 제공합니다.
 */
public class SecurityException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] args;

    public SecurityException(String message) {
        super(message);
        this.errorCode = "SECURITY_ERROR";
        this.args = null;
    }

    public SecurityException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    public SecurityException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SECURITY_ERROR";
        this.args = null;
    }

    public SecurityException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
    }

    public SecurityException(String errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}