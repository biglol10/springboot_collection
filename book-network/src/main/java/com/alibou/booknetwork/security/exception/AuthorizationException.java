package com.alibou.booknetwork.security.exception;

/**
 * 권한 관련 예외 클래스
 * 
 * 사용자 권한 검증 과정에서 발생하는 모든 예외를 처리합니다.
 * 리소스 접근, 기능 사용 권한 등의 검증에서 사용됩니다.
 */
public class AuthorizationException extends SecurityException {

    // 권한 관련 에러 코드
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String INSUFFICIENT_PRIVILEGES = "INSUFFICIENT_PRIVILEGES";
    public static final String RESOURCE_NOT_ACCESSIBLE = "RESOURCE_NOT_ACCESSIBLE";
    public static final String OPERATION_NOT_PERMITTED = "OPERATION_NOT_PERMITTED";
    public static final String ROLE_REQUIRED = "ROLE_REQUIRED";
    public static final String AUTHORITY_REQUIRED = "AUTHORITY_REQUIRED";
    public static final String OWNERSHIP_REQUIRED = "OWNERSHIP_REQUIRED";
    public static final String ADMIN_REQUIRED = "ADMIN_REQUIRED";
    public static final String BRAND_ACCESS_DENIED = "BRAND_ACCESS_DENIED";
    public static final String STORE_ACCESS_DENIED = "STORE_ACCESS_DENIED";

    public AuthorizationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public AuthorizationException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }

    public AuthorizationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public AuthorizationException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }

    // 편의 메서드들
    public static AuthorizationException accessDenied() {
        return new AuthorizationException(ACCESS_DENIED, "Access denied");
    }

    public static AuthorizationException accessDenied(String resource) {
        return new AuthorizationException(ACCESS_DENIED, 
            "Access denied to resource: %s".formatted(resource), resource);
    }

    public static AuthorizationException insufficientPrivileges() {
        return new AuthorizationException(INSUFFICIENT_PRIVILEGES, "Insufficient privileges");
    }

    public static AuthorizationException insufficientPrivileges(String operation) {
        return new AuthorizationException(INSUFFICIENT_PRIVILEGES, 
            "Insufficient privileges for operation: %s".formatted(operation), operation);
    }

    public static AuthorizationException resourceNotAccessible(String resourceId) {
        return new AuthorizationException(RESOURCE_NOT_ACCESSIBLE, 
            "Resource not accessible: %s".formatted(resourceId), resourceId);
    }

    public static AuthorizationException operationNotPermitted(String operation) {
        return new AuthorizationException(OPERATION_NOT_PERMITTED, 
            "Operation not permitted: %s".formatted(operation), operation);
    }

    public static AuthorizationException roleRequired(String requiredRole) {
        return new AuthorizationException(ROLE_REQUIRED, 
            "Role required: %s".formatted(requiredRole), requiredRole);
    }

    public static AuthorizationException authorityRequired(String requiredAuthority) {
        return new AuthorizationException(AUTHORITY_REQUIRED, 
            "Authority required: %s".formatted(requiredAuthority), requiredAuthority);
    }

    public static AuthorizationException ownershipRequired(String resourceType, String resourceId) {
        return new AuthorizationException(OWNERSHIP_REQUIRED, 
            "Ownership required for %s: %s".formatted(resourceType, resourceId), 
            resourceType, resourceId);
    }

    public static AuthorizationException adminRequired() {
        return new AuthorizationException(ADMIN_REQUIRED, "Administrator privileges required");
    }

    public static AuthorizationException brandAccessDenied(String brandCode) {
        return new AuthorizationException(BRAND_ACCESS_DENIED, 
            "Access denied to brand: %s".formatted(brandCode), brandCode);
    }

    public static AuthorizationException storeAccessDenied(String storeCode) {
        return new AuthorizationException(STORE_ACCESS_DENIED, 
            "Access denied to store: %s".formatted(storeCode), storeCode);
    }
}