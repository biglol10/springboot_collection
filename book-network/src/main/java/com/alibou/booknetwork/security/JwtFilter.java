package com.alibou.booknetwork.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * JWT 인증 필터
 * 
 * 이 필터는 HTTP 요청에서 JWT 토큰을 추출하고 검증하여 사용자 인증을 처리합니다.
 * Spring Security 필터 체인에 통합되어 보안 컨텍스트를 설정합니다.
 * OncePerRequestFilter를 상속받아 각 요청당 한 번만 실행되도록 보장합니다.
 */
@Service
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter { // OncePerRequestFilter를 상속하여 이 클래스가 필터임을 명시합니다

    private final JwtService jwtService; // JWT 토큰 처리를 위한 서비스
    private final UserDetailsService userDetailsService; // 사용자 정보를 로드하기 위한 서비스
    private final CustomAuthenticationProvider customAuthenticationProvider; // 커스텀 인증 제공자

    @Value("${spring.config.activate.on-profile}")
    private String profile;

    /**
     * 요청이 들어올 때마다 실행되는 필터 메소드
     * JWT 토큰을 검증하고 인증된 사용자 정보를 SecurityContext에 설정합니다.
     * 
     * 처리 단계:
     * 1. 인증이 필요하지 않은 경로인지 확인
     * 2. Authorization 헤더에서 JWT 토큰 추출
     * 3. 토큰에서 사용자 식별자(이메일) 추출
     * 4. 사용자 상세 정보 로드 및 토큰 유효성 검증
     * 5. 인증 객체 생성 및 보안 컨텍스트 설정
     * 
     * @param request 들어오는 HTTP 요청
     * @param response 나가는 HTTP 응답
     * @param filterChain 다음 필터로 요청을 전달하기 위한 필터 체인
     * @throws ServletException 서블릿 예외 발생 시
     * @throws IOException 입출력 예외 발생 시
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 인증이 필요하지 않은 경로(예: 로그인, 회원가입)인 경우 필터 건너뛰기
        if (request.getServletPath().contains("/api/v1/auth")) { // '/auth' 경로는 인증이 필요하지 않으므로 다음 필터로 진행
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 개발 환경에서 mock 로그인 처리
            if (handleMockLogin(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // 일반적인 JWT 토큰 인증 처리
            if (handleJwtAuthentication(request)) {
                // JWT 인증 성공 시 아무것도 하지 않음 (이미 SecurityContext에 인증 정보가 설정됨)
            }

            // 다음 필터로 요청 전달
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 모든 예외는 SecurityExceptionHandler에서 처리하도록 위임
            // 필터에서는 로깅만 수행하고 예외를 다시 던짐
            log.error("Authentication error in filter: {}", e.getMessage(), e);
            // 예외를 다시 던져서 ExceptionHandler가 처리하도록 함
            throw new RuntimeException("Authentication failed", e);
        }
    }

    /**
     * 개발 환경에서 mock 로그인 처리 (엔터프라이즈 스타일로 확장)
     * @return mock 로그인 처리 여부
     */
    private boolean handleMockLogin(HttpServletRequest request) {
        final String mockLogin = request.getHeader("mock-login");
        
        if ("dev".equals(profile) && "Y".equals(mockLogin)) {
            // Mock 헤더들 추출
            final String mockLoginEmail = request.getHeader("mock-login-email");
            final String mockUserId = request.getHeader("mock-user-id");
            final String mockRole = request.getHeader("mock-role");
            final String mockAuthorities = request.getHeader("mock-authorities");
            final String mockFirstName = request.getHeader("mock-first-name");
            final String mockLastName = request.getHeader("mock-last-name");
            
            // 기본값 설정
            String userEmail = mockLoginEmail != null && !mockLoginEmail.isEmpty() ? 
                            mockLoginEmail : "dev-user@example.com";
            Integer userId = mockUserId != null ? Integer.valueOf(mockUserId) : 999;
            String[] userRoles = mockRole != null && !mockRole.isEmpty() ? 
                            mockRole.split(",") : new String[] {"USER"};
            String[] authorities = mockAuthorities != null && !mockAuthorities.isEmpty() ? 
                            mockAuthorities.split(",") : null;
            
            // 커스텀 AuthenticationProvider를 사용한 Mock 인증
            Authentication authentication = customAuthenticationProvider.getMockAuthentication(
                userEmail, authorities, userId, userRoles
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            return true;
        }
        
        return false;
    }

    /**
     * JWT 토큰 인증 처리 (엔터프라이즈 스타일로 개선)
     * @return 인증 처리 여부
     */
    private boolean handleJwtAuthentication(HttpServletRequest request) {
        // Authorization 헤더에서 JWT 토큰 추출
        final String authHeader = request.getHeader(AUTHORIZATION);
        
        // Authorization 헤더가 없거나 Bearer 토큰 형식이 아닌 경우
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        
        // "Bearer " 접두사(7자) 이후의 문자열을 JWT 토큰으로 추출
        final String jwt = authHeader.substring(7);
        // JWT 토큰에서 사용자 이메일(식별자) 추출
        final String userEmail = jwtService.extractUsername(jwt);
        
        // 사용자가 아직 인증되지 않았고 토큰이 유효한 경우
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 이메일을 기반으로 사용자 정보 로드
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            
            // 클라이언트 IP 주소 추출
            String clientIpAddress = getClientIpAddress(request);
            
            // 엔터프라이즈급 JWT 토큰 검증 (IP 검증 포함)
            if (jwtService.isTokenValidWithIpCheck(jwt, userDetails, clientIpAddress)) {
                // JWT에서 권한 정보 추출
                java.util.List<String> authorities = jwtService.extractAuthorities(jwt);
                
                // 토큰 정보 로깅 (디버그 모드)
                if (log.isDebugEnabled()) {
                    String tokenId = jwtService.extractTokenId(jwt);
                    String tokenType = jwtService.extractTokenType(jwt);
                    log.debug("Processing token: ID={}, Type={}, User={}, IP={}", 
                             tokenId, tokenType, userEmail, clientIpAddress);
                }
                
                // 커스텀 AuthenticationProvider를 사용한 인증 객체 생성
                Authentication authentication = customAuthenticationProvider.getAuthentication(
                    userDetails, authorities
                );
                
                // 요청 상세 정보 설정 (IP, 세션 정보 등)
                if (authentication instanceof CustomAuthenticationToken) {
                    ((CustomAuthenticationToken) authentication).setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                }
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return true;
            }
        }
        
        return false;
    }

    /**
     * 클라이언트의 실제 IP 주소를 추출합니다.
     * 프록시나 로드 밸런서를 통과한 경우도 고려합니다.
     * 
     * @param request HTTP 요청 객체
     * @return 클라이언트 IP 주소
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // X-Forwarded-For 헤더 확인 (프록시/로드밸런서 통과 시)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 첫 번째 IP 주소가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }
        
        // X-Real-IP 헤더 확인
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        // Proxy-Client-IP 헤더 확인
        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if (proxyClientIp != null && !proxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(proxyClientIp)) {
            return proxyClientIp;
        }
        
        // WL-Proxy-Client-IP 헤더 확인
        String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
        if (wlProxyClientIp != null && !wlProxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
            return wlProxyClientIp;
        }
        
        // HTTP_CLIENT_IP 헤더 확인
        String httpClientIp = request.getHeader("HTTP_CLIENT_IP");
        if (httpClientIp != null && !httpClientIp.isEmpty() && !"unknown".equalsIgnoreCase(httpClientIp)) {
            return httpClientIp;
        }
        
        // HTTP_X_FORWARDED_FOR 헤더 확인
        String httpXForwardedFor = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (httpXForwardedFor != null && !httpXForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(httpXForwardedFor)) {
            return httpXForwardedFor;
        }
        
        // 기본 방법: 직접 연결된 클라이언트 IP
        return request.getRemoteAddr();
    }

    /**
     * User-Agent 정보를 추출합니다.
     * 
     * @param request HTTP 요청 객체
     * @return User-Agent 문자열
     */
    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}

/**
 * 고급 사용 사례 및 시니어 개발자를 위한 추가 정보
 * 
 * 1. 커스텀 예외 처리:
 *    - 토큰 만료, 유효하지 않은 토큰 등의 예외를 세분화하여 처리
 *    - 클라이언트에게 적절한 HTTP 상태 코드와 오류 메시지 제공
 *    - 예시:
 *      try {
 *          // 토큰 검증 로직
 *      } catch (ExpiredJwtException e) {
 *          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *          response.getWriter().write("Token expired");
 *          return;
 *      }
 * 
 * 2. 토큰 갱신 메커니즘:
 *    - 액세스 토큰이 만료되기 전에 자동 갱신
 *    - 응답 헤더에 새 토큰 포함
 *    - 만료 임계값 설정 (예: 만료 5분 전)
 * 
 * 3. 요청 매트릭스 및 로깅:
 *    - 인증 요청/실패 통계 수집
 *    - 비정상적인 패턴 감지 (잠재적 공격)
 *    - 감사 로그 생성
 * 
 * 4. 다중 토큰 지원:
 *    - 다른 클라이언트 유형에 대해 다른 토큰 유형 지원
 *    - 모바일/웹/API 클라이언트별 설정
 * 
 * 5. 토큰 저장소 통합:
 *    - Redis 또는 다른 캐시를 사용한 토큰 블랙리스트 관리
 *    - 강제 로그아웃 기능 구현
 * 
 * 6. 성능 최적화:
 *    - 토큰 검증 결과 캐싱
 *    - 사용자 세부 정보 캐싱
 *    - 비동기 처리 고려
 * 
 * 7. 보안 강화:
 *    - JWT 클레임에 요청 IP, 장치 ID 포함 및 검증
 *    - 토큰 사용 횟수 제한
 *    - 의심스러운 활동 탐지
 * 
 * 8. 분산 환경 고려:
 *    - 마이크로서비스 간 인증 정보 전파
 *    - 서비스 간 권한 검증
 *    - API 게이트웨이 통합
 */
