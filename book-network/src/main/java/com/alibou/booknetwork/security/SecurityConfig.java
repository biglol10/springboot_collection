package com.alibou.booknetwork.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * 스프링 시큐리티 보안 설정 클래스
 * 인증 및 인가 관련 전반적인 보안 설정을 담당합니다.
 * 
 * 아키텍처 개요:
 * 1. SecurityFilterChain: 요청에 대한 보안 필터링 체인을 구성합니다.
 * 2. JwtFilter: JWT 토큰 기반 인증을 처리하는 커스텀 필터입니다.
 * 3. AuthenticationProvider: 사용자 인증 로직을 제공합니다.
 * 
 * 핵심 보안 기능:
 * - JWT 기반 무상태(Stateless) 인증
 * - API 엔드포인트 접근 제어
 * - CORS 설정
 * - CSRF 방어 비활성화 (RESTful API에 적합)
 * - 메소드 수준 보안
 * 
 * 확장 가능성:
 * - OAuth2/소셜 로그인 통합
 * - MFA(다중 인증) 추가
 * - 커스텀 보안 메타데이터 처리
 * - 고급 인가 규칙 적용
 * 
 * 모범 사례:
 * - 최소 권한 원칙 적용
 * - 보안 헤더 적용
 * - 요청 비율 제한 (Rate Limiting)
 * - 적절한 예외 처리
 */
@Configuration // 스프링의 설정 클래스임을 표시합니다.
@EnableWebSecurity // 스프링 시큐리티의 웹 보안 지원을 활성화하고 스프링 MVC 통합 기능을 제공합니다.
@RequiredArgsConstructor // 필수 필드에 대한 생성자를 자동으로 생성합니다(final 필드들).
@EnableMethodSecurity(securedEnabled = true) // 메소드 레벨 보안을 활성화합니다. @Secured 어노테이션 사용을 가능하게 합니다.
public class SecurityConfig {
    private final JwtFilter jwtAuthFilter; // JWT 인증 필터 - 요청의 JWT 토큰을 검증합니다.
    private final AuthenticationProvider authenticationProvider; // 사용자 인증 제공자 - 실제 인증 로직을 처리합니다.

    /**
     * 스프링 시큐리티 필터 체인 설정
     * 보안 규칙과 필터 순서를 정의합니다.
     * 
     * 동작 방식:
     * 1. 스프링이 시작될 때 @Configuration 클래스를 스캔합니다.
     * 2. @Bean 어노테이션이 붙은 메소드를 찾아 스프링 컨텍스트에 등록합니다.
     * 3. 이 메소드에서 정의된 보안 설정이 애플리케이션 전체에 적용됩니다.
     * 
     * 사용 사례:
     * - REST API 보안: 토큰 기반 인증으로 API 엔드포인트 보호
     * - 공개/비공개 리소스 관리: 일부 경로는 공개, 나머지는 인증 필요
     * - 개발 도구 접근 제어: Swagger UI와 같은 API 문서화 도구에 대한 접근 관리
     * 
     * 고급 기능 구현 방법:
     * 1. OAuth2 통합:
     *    http.oauth2Login(oauth2 -> oauth2
     *        .loginPage("/login")
     *        .userInfoEndpoint(userInfo -> userInfo
     *            .userService(oAuth2UserService)));
     * 
     * 2. 보안 헤더 추가:
     *    http.headers(headers -> headers
     *        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
     *        .xssProtection(xss -> xss.enable(true))
     *        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")));
     * 
     * 3. 요청 비율 제한:
     *    - 커스텀 필터 구현 후 다음과 같이 추가:
     *    http.addFilterBefore(ratelimitFilter, UsernamePasswordAuthenticationFilter.class);
     * 
     * 4. CORS 상세 설정:
     *    http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
     *    
     *    @Bean
     *    CorsConfigurationSource corsConfigurationSource() {
     *        CorsConfiguration configuration = new CorsConfiguration();
     *        configuration.setAllowedOrigins(Arrays.asList("https://example.com"));
     *        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
     *        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
     *        source.registerCorsConfiguration("/**", configuration);
     *        return source;
     *    }
     * 
     * 5. 역할 기반 접근 제어 세분화:
     *    http.authorizeRequests(req ->
     *        req.requestMatchers("/api/admin/**").hasRole("ADMIN")
     *           .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
     *           .requestMatchers("/api/public/**").permitAll());
     * 
     * 보안 권장 사항:
     * - 토큰 만료 시간을 적절하게 설정 (너무 길지 않게)
     * - JWT 서명 키를 안전하게 관리 (환경 변수 또는 보안 저장소 사용)
     * - 정기적인 보안 취약점 검사 실시
     * - 로깅 및 모니터링 구현으로 잠재적 공격 감지
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults()) // Cross-Origin Resource Sharing 설정을 기본값으로 활성화합니다.
                .csrf(AbstractHttpConfigurer::disable) // REST API는 무상태이므로 CSRF 보호를 비활성화합니다.
                .authorizeRequests(req ->
                            req.requestMatchers(
                                            "/auth/**", // 인증 관련 엔드포인트
                                            "/v2/api-docs", // Swagger API 문서 관련 경로들
                                            "/v3/api-docs",
                                            "/v3/api-docs/**",
                                            "/swagger-resources",
                                            "/swagger-resources/**",
                                            "/configuration/ui",
                                            "/configuration/security",
                                            "/swagger-ui/**",
                                            "/webjars/**",
                                            "/swagger-ui.html"
                            ).permitAll() // 위의 경로들은 인증 없이 모든 사용자에게 허용됩니다.
                                    .anyRequest().authenticated() // 그 외 모든 요청은 인증이 필요합니다.
                        )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 기반 인증이므로 세션을 생성하지 않습니다(무상태 방식).
                .authenticationProvider(authenticationProvider) // 사용자 인증을 처리할 제공자를 설정합니다.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // UsernamePasswordAuthenticationFilter 전에 JWT 필터를 추가하여 토큰을 먼저 검증합니다.
        return http.build();
    }
    
    /**
     * 시니어 개발자로 발전하기 위한 추가 고려사항:
     * 
     * 1. 멀티 테넌시(Multi-tenancy) 구현:
     *    - 테넌트별 보안 정책 분리
     *    - 테넌트 정보를 JWT 클레임에 포함
     *    - 동적 보안 규칙 적용
     * 
     * 2. 고급 인증 메커니즘:
     *    - 생체 인증 통합
     *    - 하드웨어 토큰 지원
     *    - 리스크 기반 인증 전략
     * 
     * 3. 마이크로서비스 아키텍처 보안:
     *    - API 게이트웨이 통합
     *    - 서비스 간 인증 관리
     *    - 분산 토큰 검증
     * 
     * 4. 보안 감사 및 모니터링:
     *    - 중요 작업 감사 추적
     *    - 이상 행동 감지
     *    - 보안 이벤트 실시간 알림
     * 
     * 5. 확장성을 위한 모듈화:
     *    - 보안 컴포넌트 모듈화
     *    - 프로필 기반 보안 설정
     *    - 환경별 구성 자동화
     * 
     * 6. 제로 트러스트 보안 모델 채택:
     *    - 모든 요청 지속적 검증
     *    - 최소 권한 원칙 강화
     *    - 컨텍스트 인식 접근 제어
     */
}
