package com.alibou.booknetwork.security.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * 보안 관련 글로벌 예외 핸들러
 * 
 * 모든 보안 관련 예외를 포착하여 표준화된 응답을 제공합니다.
 * SSGD 스타일의 엔터프라이즈급 예외 처리를 구현합니다.
 */
@Slf4j
@RestControllerAdvice
public class SecurityExceptionHandler {

    /**
     * 커스텀 보안 예외 처리
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<SecurityErrorResponse> handleSecurityException(
            SecurityException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String path = request.getRequestURI();
        
        log.warn("Security exception occurred - TraceId: {}, Path: {}, Error: {}", 
                traceId, path, ex.getMessage(), ex);
        
        SecurityErrorResponse response = SecurityErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                path
        ).withTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 토큰 예외 처리
     */
    @ExceptionHandler(TokenException.class)
    public ResponseEntity<SecurityErrorResponse> handleTokenException(
            TokenException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String path = request.getRequestURI();
        
        // 토큰 만료는 INFO 레벨로, 나머지는 WARN 레벨로 로깅
        if (TokenException.TOKEN_EXPIRED.equals(ex.getErrorCode())) {
            log.info("Token expired - TraceId: {}, Path: {}", traceId, path);
        } else {
            log.warn("Token exception occurred - TraceId: {}, Path: {}, Error: {}", 
                    traceId, path, ex.getMessage(), ex);
        }
        
        SecurityErrorResponse response = SecurityErrorResponse.tokenError(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                path
        ).withTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 커스텀 인증 예외 처리
     */
    @ExceptionHandler(com.alibou.booknetwork.security.exception.AuthenticationException.class)
    public ResponseEntity<SecurityErrorResponse> handleCustomAuthenticationException(
            com.alibou.booknetwork.security.exception.AuthenticationException ex, 
            HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String path = request.getRequestURI();
        
        log.warn("Authentication exception occurred - TraceId: {}, Path: {}, Error: {}", 
                traceId, path, ex.getMessage(), ex);
        
        SecurityErrorResponse response = SecurityErrorResponse.authenticationError(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                path
        ).withTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 권한 예외 처리
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<SecurityErrorResponse> handleAuthorizationException(
            AuthorizationException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String path = request.getRequestURI();
        
        log.warn("Authorization exception occurred - TraceId: {}, Path: {}, Error: {}", 
                traceId, path, ex.getMessage(), ex);
        
        SecurityErrorResponse response = SecurityErrorResponse.authorizationError(
                HttpStatus.FORBIDDEN.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                path
        ).withTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Spring Security AccessDeniedException 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<SecurityErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String path = request.getRequestURI();
        
        log.warn("Access denied - TraceId: {}, Path: {}, Error: {}", 
                traceId, path, ex.getMessage());
        
        SecurityErrorResponse response = SecurityErrorResponse.authorizationError(
                HttpStatus.FORBIDDEN.value(),
                AuthorizationException.ACCESS_DENIED,
                "Access denied to the requested resource",
                path
        ).withTraceId(traceId)
         .withDetails(ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Spring Security AuthenticationException 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<SecurityErrorResponse> handleSpringAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String path = request.getRequestURI();
        
        log.warn("Spring authentication exception - TraceId: {}, Path: {}, Error: {}", 
                traceId, path, ex.getMessage());
        
        SecurityErrorResponse response = SecurityErrorResponse.authenticationError(
                HttpStatus.UNAUTHORIZED.value(),
                com.alibou.booknetwork.security.exception.AuthenticationException.AUTHENTICATION_FAILED,
                "Authentication failed",
                path
        ).withTraceId(traceId)
         .withDetails(ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * JWT 만료 예외 처리
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<SecurityErrorResponse> handleExpiredJwtException(
            ExpiredJwtException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String path = request.getRequestURI();
        
        log.info("JWT token expired - TraceId: {}, Path: {}", traceId, path);
        
        SecurityErrorResponse response = SecurityErrorResponse.tokenError(
                HttpStatus.UNAUTHORIZED.value(),
                TokenException.TOKEN_EXPIRED,
                "JWT token has expired",
                path
        ).withTraceId(traceId)
         .withAdditionalInfo("expiredAt", ex.getClaims().getExpiration());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * JWT 형식 오류 예외 처리
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<SecurityErrorResponse> handleMalformedJwtException(
            MalformedJwtException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String path = request.getRequestURI();
        
        log.warn("Malformed JWT token - TraceId: {}, Path: {}, Error: {}", 
                traceId, path, ex.getMessage());
        
        SecurityErrorResponse response = SecurityErrorResponse.tokenError(
                HttpStatus.BAD_REQUEST.value(),
                TokenException.TOKEN_MALFORMED,
                "JWT token is malformed",
                path
        ).withTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * JWT 서명 오류 예외 처리
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<SecurityErrorResponse> handleSignatureException(
            SignatureException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String path = request.getRequestURI();
        
        log.warn("Invalid JWT signature - TraceId: {}, Path: {}, Error: {}", 
                traceId, path, ex.getMessage());
        
        SecurityErrorResponse response = SecurityErrorResponse.tokenError(
                HttpStatus.UNAUTHORIZED.value(),
                TokenException.TOKEN_SIGNATURE_INVALID,
                "JWT token signature is invalid",
                path
        ).withTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * JWT 지원되지 않는 형식 예외 처리
     */
    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<SecurityErrorResponse> handleUnsupportedJwtException(
            UnsupportedJwtException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        String path = request.getRequestURI();
        
        log.warn("Unsupported JWT token - TraceId: {}, Path: {}, Error: {}", 
                traceId, path, ex.getMessage());
        
        SecurityErrorResponse response = SecurityErrorResponse.tokenError(
                HttpStatus.BAD_REQUEST.value(),
                TokenException.TOKEN_UNSUPPORTED,
                "JWT token format is not supported",
                path
        ).withTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 일반적인 JWT 관련 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SecurityErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        // JWT 관련 IllegalArgumentException만 처리
        if (ex.getMessage() != null && ex.getMessage().contains("JWT")) {
            String traceId = generateTraceId();
            String path = request.getRequestURI();
            
            log.warn("Invalid JWT argument - TraceId: {}, Path: {}, Error: {}", 
                    traceId, path, ex.getMessage());
            
            SecurityErrorResponse response = SecurityErrorResponse.tokenError(
                    HttpStatus.BAD_REQUEST.value(),
                    TokenException.TOKEN_INVALID,
                    "Invalid JWT token",
                    path
            ).withTraceId(traceId);
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // JWT 관련이 아닌 경우 다른 핸들러에서 처리하도록 예외를 다시 던짐
        throw ex;
    }

    /**
     * 추적 ID 생성
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}