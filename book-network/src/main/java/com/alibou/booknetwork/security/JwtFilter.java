package com.alibou.booknetwork.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

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
        
        // Authorization 헤더에서 JWT 토큰 추출
        final String authHeader = request.getHeader(AUTHORIZATION);
        final String jwt;
        final String userEmail;

        // Authorization 헤더가 없거나 Bearer 토큰 형식이 아닌 경우 다음 필터로 진행
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // "Bearer " 접두사(7자) 이후의 문자열을 JWT 토큰으로 추출
        jwt = authHeader.substring(7);
        // JWT 토큰에서 사용자 이메일(식별자) 추출
        userEmail = jwtService.extractUsername(jwt);

        /**
         * SecurityContextHolder.getContext().getAuthentication() == null은 
         * 현재 스레드의 보안 컨텍스트에 인증 정보가 없는지 확인하는 조건입니다.
         * 
         * SecurityContextHolder: Spring Security에서 현재 인증된 사용자의 정보를 저장하는 핵심 클래스
         * getContext(): 현재 스레드와 연관된 SecurityContext를 반환
         * getAuthentication(): SecurityContext에서 Authentication 객체를 반환, 인증 정보가 없으면 null 반환
         * 
         * 이 조건은 사용자가 아직 인증되지 않았는지 확인하여 중복 인증을 방지합니다.
         */
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 이메일을 기반으로 사용자 정보 로드
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            
            // JWT 토큰이 유효한지 검증
            if (jwtService.isTokenValid(jwt, userDetails)) {
                /**
                 * UsernamePasswordAuthenticationToken은 Spring Security에서 사용하는 인증 객체입니다.
                 * 이 객체는 인증된 사용자의 주요 정보와 권한을 담고 있습니다.
                 * 
                 * 매개변수:
                 * 1. userDetails: 사용자의 식별자, 비밀번호, 권한 등의 정보를 포함
                 * 2. null: 자격 증명(일반적으로 비밀번호)을 나타내며, 여기서는 이미 인증이 완료되었으므로 null 사용
                 * 3. userDetails.getAuthorities(): 사용자가 가진 권한(roles, authorities) 목록
                 */
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                /**
                 * 사용자 정의 UserDetails 사용 시 참고사항:
                 * 
                 * UserDetailsService.loadUserByUsername()이 CustomUserDetails 객체를 반환하는 경우,
                 * SecurityContextHolder.getContext().getAuthentication().getPrincipal()을 통해
                 * CustomUserDetails 객체를 얻을 수 있습니다.
                 * 
                 * 예시:
                 * CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
                 */

                // 요청 정보를 인증 토큰에 추가 (로깅, 감사 등에 유용)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // 생성된 인증 토큰을 SecurityContext에 설정하여 사용자를 인증 상태로 만듦
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
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
