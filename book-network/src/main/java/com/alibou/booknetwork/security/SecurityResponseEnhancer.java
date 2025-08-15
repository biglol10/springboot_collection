package com.alibou.booknetwork.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 보안 응답 강화 클래스
 * 
 * 모든 API 응답에 보안 헤더와 추가 정보를 포함시킵니다.
 * SSGD 스타일의 엔터프라이즈급 보안 응답 처리를 구현합니다.
 */
@Slf4j
@RestControllerAdvice
public class SecurityResponseEnhancer implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, 
                          Class<? extends HttpMessageConverter<?>> converterType) {
        // 모든 응답에 대해 처리 적용
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, 
                                MethodParameter returnType,
                                MediaType selectedContentType, 
                                Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                ServerHttpRequest request, 
                                ServerHttpResponse response) {
        
        // 보안 헤더 추가
        addSecurityHeaders(response);
        
        // API 응답에 추가 보안 정보 포함 (보안 관련 엔드포인트만)
        if (isSecurityEndpoint(request.getURI().getPath())) {
            addSecurityInfo(response, request);
        }
        
        return body;
    }

    /**
     * 보안 헤더 추가
     */
    private void addSecurityHeaders(ServerHttpResponse response) {
        // CORS 보안 헤더
        response.getHeaders().add("X-Content-Type-Options", "nosniff");
        response.getHeaders().add("X-Frame-Options", "DENY");
        response.getHeaders().add("X-XSS-Protection", "1; mode=block");
        response.getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // CSP 헤더 (API 서버용)
        response.getHeaders().add("Content-Security-Policy", 
            "default-src 'none'; frame-ancestors 'none'");
        
        // 캐시 제어 (보안 민감 정보는 캐시하지 않음)
        response.getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
        response.getHeaders().add("Pragma", "no-cache");
        response.getHeaders().add("Expires", "0");
        
        // HSTS (HTTPS 환경에서만 유효)
        response.getHeaders().add("Strict-Transport-Security", 
            "max-age=31536000; includeSubDomains");
        
        // 추가 보안 헤더
        response.getHeaders().add("X-Permitted-Cross-Domain-Policies", "none");
        response.getHeaders().add("X-Download-Options", "noopen");
    }

    /**
     * 보안 관련 추가 정보 헤더 추가
     */
    private void addSecurityInfo(ServerHttpResponse response, ServerHttpRequest request) {
        // 요청 추적 정보
        response.getHeaders().add("X-Request-ID", generateRequestId());
        
        // API 버전 정보
        response.getHeaders().add("X-API-Version", "1.0");
        
        // 보안 정책 버전
        response.getHeaders().add("X-Security-Policy-Version", "2024.1");
        
        // 서버 시간 (UTC)
        response.getHeaders().add("X-Server-Time", 
            java.time.Instant.now().toString());
    }

    /**
     * 보안 엔드포인트 여부 확인
     */
    private boolean isSecurityEndpoint(String path) {
        return path != null && (
            path.startsWith("/api/v1/auth") ||
            path.startsWith("/api/v1/security-test") ||
            path.contains("login") ||
            path.contains("token") ||
            path.contains("auth")
        );
    }

    /**
     * 요청 ID 생성
     */
    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}