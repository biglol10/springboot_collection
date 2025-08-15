package com.alibou.booknetwork.security;

import com.alibou.booknetwork.security.exception.AuthenticationException;
import com.alibou.booknetwork.security.exception.AuthorizationException;
import com.alibou.booknetwork.security.exception.TokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 보안 테스트용 컨트롤러
 * 
 * CustomAuthenticationProvider가 제대로 작동하는지 테스트하기 위한 임시 컨트롤러입니다.
 * 개발 환경에서만 사용하며, 프로덕션에서는 제거해야 합니다.
 */
@RestController
@RequestMapping("/api/v1/security-test")
@RequiredArgsConstructor
public class SecurityTestController {

    private final JwtService jwtService;

    /**
     * 현재 인증된 사용자 정보를 반환하는 엔드포인트
     * Mock 로그인과 실제 JWT 인증이 모두 잘 작동하는지 확인할 수 있습니다.
     * 
     * @return 인증 정보 맵
     */
    @GetMapping("/current-user")
    public Map<String, Object> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null) {
            response.put("authenticated", false);
            response.put("message", "No authentication found");
            return response;
        }
        
        response.put("authenticated", authentication.isAuthenticated());
        response.put("authType", authentication.getClass().getSimpleName());
        response.put("principal", authentication.getPrincipal());
        response.put("authorities", authentication.getAuthorities());
        
        // CustomAuthenticationToken인지 확인
        if (authentication instanceof CustomAuthenticationToken) {
            response.put("customAuth", true);
            response.put("details", authentication.getDetails());
        } else {
            response.put("customAuth", false);
        }
        
        return response;
    }

    /**
     * 권한 체크 테스트 엔드포인트
     * 
     * @return 권한 정보
     */
    @GetMapping("/check-authorities")
    public Map<String, Object> checkAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null) {
            response.put("error", "No authentication");
            return response;
        }
        
        response.put("allAuthorities", AuthorityUtils.getCurrentUserAuthorities());
        
        // 도서 관련 권한
        response.put("canAccessBooks", AuthorityUtils.canAccessBooks());
        response.put("canModifyBooks", AuthorityUtils.canModifyBooks());
        response.put("hasBookRead", AuthorityUtils.hasAuthority(BookAuthorities.BOOK_READ));
        response.put("hasBookCUD", AuthorityUtils.hasAuthority(BookAuthorities.BOOK_CUD));
        response.put("hasBookCRUD", AuthorityUtils.hasAuthority(BookAuthorities.BOOK_CRUD));
        
        // 사용자 관리 권한
        response.put("canManageUsers", AuthorityUtils.canManageUsers());
        response.put("hasUserRead", AuthorityUtils.hasAuthority(BookAuthorities.USER_READ));
        response.put("hasUserManage", AuthorityUtils.hasAuthority(BookAuthorities.USER_MANAGE));
        
        // 거래 관련 권한
        response.put("hasTransactionRead", AuthorityUtils.hasAuthority(BookAuthorities.TRANSACTION_READ));
        response.put("hasTransactionBorrow", AuthorityUtils.hasAuthority(BookAuthorities.TRANSACTION_BORROW));
        response.put("hasTransactionReturn", AuthorityUtils.hasAuthority(BookAuthorities.TRANSACTION_RETURN));
        response.put("hasTransactionManage", AuthorityUtils.hasAuthority(BookAuthorities.TRANSACTION_MANAGE));
        
        // 관리자 권한
        response.put("isAdmin", AuthorityUtils.isAdmin());
        response.put("isSuperAdmin", AuthorityUtils.isSuperAdmin());
        response.put("canAccessSystem", AuthorityUtils.canAccessSystem());
        
        // 시스템 권한
        response.put("hasSystemRead", AuthorityUtils.hasAuthority(BookAuthorities.SYSTEM_READ));
        response.put("hasSystemManage", AuthorityUtils.hasAuthority(BookAuthorities.SYSTEM_MANAGE));
        response.put("hasAdminAll", AuthorityUtils.hasAuthority(BookAuthorities.ADMIN_ALL));
        
        return response;
    }

    /**
     * 역할별 권한 테스트 엔드포인트
     * 
     * @return 역할별 권한 정보
     */
    @GetMapping("/test-roles")
    public Map<String, Object> testRoles() {
        Map<String, Object> response = new HashMap<>();
        
        // 각 역할별 권한 목록 반환
        response.put("USER_AUTHORITIES", BookAuthorities.getAuthoritiesByRole("USER"));
        response.put("BOOK_MANAGER_AUTHORITIES", BookAuthorities.getAuthoritiesByRole("BOOK_MANAGER"));
        response.put("SYSTEM_ADMIN_AUTHORITIES", BookAuthorities.getAuthoritiesByRole("SYSTEM_ADMIN"));
        response.put("SUPER_ADMIN_AUTHORITIES", BookAuthorities.getAuthoritiesByRole("SUPER_ADMIN"));
        response.put("ALL_AUTHORITIES", BookAuthorities.getAllAuthorities());
        
        return response;
    }

    /**
     * 권한 디버그 정보 제공
     * 
     * @return 디버그 정보
     */
    @GetMapping("/debug")
    public Map<String, Object> debugAuthorities() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("debugInfo", AuthorityUtils.debugCurrentUserAuthorities());
        response.put("currentUser", AuthorityUtils.getCurrentUsername());
        response.put("allUserAuthorities", AuthorityUtils.getCurrentUserAuthorities());
        
        return response;
    }

    /**
     * JWT 토큰 정보 분석 엔드포인트
     * 
     * @param authorization Authorization 헤더
     * @param request HTTP 요청 객체
     * @return JWT 토큰 상세 정보
     */
    @GetMapping("/jwt-info")
    public Map<String, Object> getJwtInfo(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.put("error", "No Bearer token found");
            return response;
        }
        
        String token = authorization.substring(7);
        
        try {
            // JWT 토큰 상세 정보 추출
            Map<String, Object> tokenInfo = jwtService.getTokenInfo(token);
            response.put("tokenInfo", tokenInfo);
            
            // 추가 검증 정보
            response.put("tokenId", jwtService.extractTokenId(token));
            response.put("username", jwtService.extractUsername(token));
            response.put("authorities", jwtService.extractAuthorities(token));
            response.put("userId", jwtService.extractUserId(token));
            response.put("tokenType", jwtService.extractTokenType(token));
            response.put("ipAddress", jwtService.extractIpAddress(token));
            response.put("userAgent", jwtService.extractUserAgent(token));
            
            // 토큰 상태 확인
            response.put("isExpired", jwtService.isTokenExpired(token));
            response.put("isNearExpiry", jwtService.isTokenNearExpiry(token, 15));
            response.put("isAccessToken", jwtService.isTokenTypeValid(token, JwtService.TOKEN_TYPE_ACCESS));
            
            // 클라이언트 정보
            response.put("clientIP", getClientIpAddress(request));
            response.put("clientUserAgent", request.getHeader("User-Agent"));
            
        } catch (Exception e) {
            response.put("error", "Failed to parse token: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 토큰 검증 테스트 엔드포인트
     * 
     * @param authorization Authorization 헤더
     * @param request HTTP 요청 객체
     * @return 토큰 검증 결과
     */
    @GetMapping("/validate-token")
    public Map<String, Object> validateToken(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.put("valid", false);
            response.put("error", "No Bearer token found");
            return response;
        }
        
        String token = authorization.substring(7);
        String clientIp = getClientIpAddress(request);
        
        try {
            // 기본 토큰 검증
            String username = jwtService.extractUsername(token);
            response.put("username", username);
            response.put("basicValidation", username != null);
            
            // 만료 확인
            response.put("expired", jwtService.isTokenExpired(token));
            
            // 토큰 타입 확인
            String tokenType = jwtService.extractTokenType(token);
            response.put("tokenType", tokenType);
            response.put("isAccessToken", JwtService.TOKEN_TYPE_ACCESS.equals(tokenType));
            
            // IP 주소 검증
            String tokenIp = jwtService.extractIpAddress(token);
            response.put("tokenIP", tokenIp);
            response.put("clientIP", clientIp);
            response.put("ipMatch", tokenIp == null || tokenIp.equals(clientIp));
            
            // 전체 검증 결과
            response.put("valid", !jwtService.isTokenExpired(token) && 
                                 username != null && 
                                 JwtService.TOKEN_TYPE_ACCESS.equals(tokenType));
            
        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    /**
     * 클라이언트 IP 주소 추출 (JwtFilter와 동일한 로직)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    // ================================
    // 예외 처리 테스트 엔드포인트들
    // ================================

    /**
     * 토큰 예외 테스트 엔드포인트
     * 
     * @param exceptionType 예외 타입
     * @return 예외 발생
     */
    @GetMapping("/test-exception/token/{exceptionType}")
    public Map<String, Object> testTokenException(@PathVariable String exceptionType) {
        switch (exceptionType.toLowerCase()) {
            case "expired":
                throw TokenException.expired("Test token expired exception");
            case "invalid":
                throw TokenException.invalid("Test token invalid exception");
            case "malformed":
                throw TokenException.malformed("Test token malformed exception");
            case "signature":
                throw TokenException.signatureInvalid("Test token signature invalid exception");
            case "unsupported":
                throw TokenException.unsupported("Test token unsupported exception");
            case "missing":
                throw TokenException.missing("Test token missing exception");
            case "blacklisted":
                throw TokenException.blacklisted("test-token-id-123");
            case "wrongtype":
                throw TokenException.wrongType("ACCESS", "REFRESH");
            case "ipmismatch":
                throw TokenException.ipMismatch("192.168.1.100", "192.168.1.200");
            default:
                throw TokenException.invalid("Unknown token exception type: " + exceptionType);
        }
    }

    /**
     * 인증 예외 테스트 엔드포인트
     * 
     * @param exceptionType 예외 타입
     * @return 예외 발생
     */
    @GetMapping("/test-exception/auth/{exceptionType}")
    public Map<String, Object> testAuthenticationException(@PathVariable String exceptionType) {
        switch (exceptionType.toLowerCase()) {
            case "usernotfound":
                throw AuthenticationException.userNotFound("test-user@example.com");
            case "userdisabled":
                throw AuthenticationException.userDisabled("test-user@example.com");
            case "userlocked":
                throw AuthenticationException.userLocked("test-user@example.com");
            case "credentialsexpired":
                throw AuthenticationException.credentialsExpired("test-user@example.com");
            case "accountexpired":
                throw AuthenticationException.accountExpired("test-user@example.com");
            case "invalidcredentials":
                throw AuthenticationException.invalidCredentials();
            case "authrequired":
                throw AuthenticationException.authenticationRequired();
            case "authfailed":
                throw AuthenticationException.authenticationFailed("Invalid password");
            case "alreadyauth":
                throw AuthenticationException.alreadyAuthenticated();
            case "mockdisabled":
                throw AuthenticationException.mockLoginDisabled();
            default:
                throw AuthenticationException.authenticationFailed("Unknown auth exception type: " + exceptionType);
        }
    }

    /**
     * 권한 예외 테스트 엔드포인트
     * 
     * @param exceptionType 예외 타입
     * @return 예외 발생
     */
    @GetMapping("/test-exception/authz/{exceptionType}")
    public Map<String, Object> testAuthorizationException(@PathVariable String exceptionType) {
        switch (exceptionType.toLowerCase()) {
            case "accessdenied":
                throw AuthorizationException.accessDenied();
            case "accessdeniedresource":
                throw AuthorizationException.accessDenied("/api/v1/admin/users");
            case "insufficient":
                throw AuthorizationException.insufficientPrivileges();
            case "insufficientop":
                throw AuthorizationException.insufficientPrivileges("DELETE_USER");
            case "resourcenotaccessible":
                throw AuthorizationException.resourceNotAccessible("book-123");
            case "operationnotpermitted":
                throw AuthorizationException.operationNotPermitted("MODIFY_SYSTEM_CONFIG");
            case "rolerequired":
                throw AuthorizationException.roleRequired("ADMIN");
            case "authorityrequired":
                throw AuthorizationException.authorityRequired("BOOK_WRITE");
            case "ownershiprequired":
                throw AuthorizationException.ownershipRequired("Book", "book-456");
            case "adminrequired":
                throw AuthorizationException.adminRequired();
            case "brandaccessdenied":
                throw AuthorizationException.brandAccessDenied("SAMSUNG");
            case "storeaccessdenied":
                throw AuthorizationException.storeAccessDenied("STORE001");
            default:
                throw AuthorizationException.accessDenied("Unknown authz exception type: " + exceptionType);
        }
    }

    /**
     * 일반 Spring Security 예외 테스트
     * 
     * @return 예외 발생
     */
    @GetMapping("/test-exception/spring-security")
    public Map<String, Object> testSpringSecurityException() {
        throw new org.springframework.security.access.AccessDeniedException(
            "Test Spring Security AccessDeniedException");
    }

    /**
     * JWT 라이브러리 예외 테스트
     * 
     * @param exceptionType 예외 타입
     * @return 예외 발생
     */
    @GetMapping("/test-exception/jwt/{exceptionType}")
    public Map<String, Object> testJwtLibraryException(@PathVariable String exceptionType) {
        switch (exceptionType.toLowerCase()) {
            case "expired":
                throw new io.jsonwebtoken.ExpiredJwtException(null, null, "Test expired JWT");
            case "malformed":
                throw new io.jsonwebtoken.MalformedJwtException("Test malformed JWT");
            case "signature":
                throw new io.jsonwebtoken.security.SignatureException("Test signature exception");
            case "unsupported":
                throw new io.jsonwebtoken.UnsupportedJwtException("Test unsupported JWT");
            case "illegalarg":
                throw new IllegalArgumentException("JWT claims string is empty");
            default:
                throw new IllegalArgumentException("Unknown JWT exception type: " + exceptionType);
        }
    }

    /**
     * 예외 처리 테스트 목록 반환
     * 
     * @return 테스트 가능한 예외 목록
     */
    @GetMapping("/test-exception/list")
    public Map<String, Object> getExceptionTestList() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("tokenExceptions", new String[]{
            "expired", "invalid", "malformed", "signature", "unsupported", 
            "missing", "blacklisted", "wrongtype", "ipmismatch"
        });
        
        response.put("authExceptions", new String[]{
            "usernotfound", "userdisabled", "userlocked", "credentialsexpired", 
            "accountexpired", "invalidcredentials", "authrequired", "authfailed", 
            "alreadyauth", "mockdisabled"
        });
        
        response.put("authzExceptions", new String[]{
            "accessdenied", "accessdeniedresource", "insufficient", "insufficientop", 
            "resourcenotaccessible", "operationnotpermitted", "rolerequired", 
            "authorityrequired", "ownershiprequired", "adminrequired", 
            "brandaccessdenied", "storeaccessdenied"
        });
        
        response.put("jwtExceptions", new String[]{
            "expired", "malformed", "signature", "unsupported", "illegalarg"
        });
        
        response.put("springSecurityExceptions", new String[]{
            "spring-security"
        });
        
        return response;
    }
}