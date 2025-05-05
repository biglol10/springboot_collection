package com.alibou.booknetwork.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 애플리케이션 빈 설정 클래스
 * 
 * 이 클래스는 Spring 애플리케이션에서 필요한 다양한 빈(Bean)들을 정의합니다.
 * 주로 Spring Security 인증 관련 컴포넌트와 유틸리티 빈들을 설정합니다.
 * 이러한 빈들은 애플리케이션 전체에서 의존성 주입을 통해 사용됩니다.
 */
@Configuration // 이 클래스가 Spring 설정 클래스임을 나타냅니다
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성합니다
/**
 * 스프링에서 의존성 주입은 다음과 같이 작동합니다:
스프링은 애플리케이션이 시작될 때 빈(Bean)이라 불리는 객체들을 생성하고 관리합니다. 빈은 주로 @Component, @Service, @Repository, @Controller, @Configuration 등의 어노테이션이 붙은 클래스들입니다.
하나의 빈이 다른 빈에 의존할 때(예: SecurityConfig가 JwtFilter와 AuthenticationProvider에 의존), 스프링은 그 의존성을 "주입"해 줍니다.
의존성 주입 방법에는 여러 가지가 있는데, 생성자 주입(Constructor Injection)이 가장 권장되는 방식입니다.
@RequiredArgsConstructor가 생성하는 생성자는 스프링이 의존성 주입에 사용합니다. 예를 들어:

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final AuthenticationProvider authenticationProvider;
    
    // Lombok이 자동으로 생성자를 만들어줌
}
    스프링은 SecurityConfig 객체를 생성할 때:
JwtFilter 타입의 빈을 찾습니다.
AuthenticationProvider 타입의 빈을 찾습니다.
이 두 빈을 파라미터로 사용하여 SecurityConfig의 생성자를 호출합니다.
이렇게 SecurityConfig는 필요한 의존성들을 스프링으로부터 자동으로 주입받게 됩니다. 이 과정에서 @RequiredArgsConstructor가 생성해준 생성자가 사용되는 것입니다.
 */
public class BeansConfig {
    // 기본 UserDetailsService 구현 대신 커스텀 구현체(UserDetailsServiceImpl)를 사용합니다
    // 이를 통해 사용자 정보를 데이터베이스에서 로드하는 방식을 커스터마이징할 수 있습니다
    private final UserDetailsService userDetailsService;

    /**
     * 인증 제공자(AuthenticationProvider) 빈을 설정합니다.
     * 
     * DaoAuthenticationProvider는 UserDetailsService를 사용하여 사용자 정보를 로드하고,
     * PasswordEncoder를 사용하여 비밀번호를 검증합니다.
     * SecurityConfig에서 이 빈을 주입받아 사용합니다.
     * 
     * @return 구성된 AuthenticationProvider 객체
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // DaoAuthenticationProvider는 AuthenticationProvider의 구체적 구현체입니다
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // 사용자 정보를 로드할 UserDetailsService 설정
        authProvider.setUserDetailsService(userDetailsService);
        // 비밀번호 검증에 사용할 PasswordEncoder 설정
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 비밀번호 인코더(PasswordEncoder) 빈을 설정합니다.
     * 
     * BCryptPasswordEncoder는 BCrypt 해싱 함수를 사용하여 비밀번호를 안전하게 해시화합니다.
     * 이를 통해 데이터베이스에 평문 비밀번호가 아닌 해시된 비밀번호를 저장할 수 있습니다.
     * 
     * @return 비밀번호 인코딩을 위한 BCryptPasswordEncoder 객체
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 인증 관리자(AuthenticationManager) 빈을 설정합니다.
     * 
     * AuthenticationManager는 Spring Security의 인증 처리를 담당하는 핵심 인터페이스입니다.
     * 이 빈은 주로 인증 서비스에서 사용자 로그인 처리에 사용됩니다.
     * 
     * @param config AuthenticationConfiguration 객체
     * @return AuthenticationManager 객체
     * @throws Exception 인증 관리자 생성 중 오류 발생 시
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 감사(Audit) 정보 제공자 빈을 설정합니다.
     * 
     * AuditorAware는 Spring Data JPA의 감사 기능을 위해 사용됩니다.
     * 엔티티의 생성자, 수정자 정보를 자동으로 기록하는 데 활용됩니다.
     * ApplicationAuditAware 클래스는 현재 인증된 사용자의 ID를 제공합니다.
     * 
     * @return 감사 정보를 제공하는 AuditorAware 객체
     */
    @Bean
    public AuditorAware<Integer> auditorAware() {
        return new ApplicationAuditAware();
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 다양한 인증 방식 지원:
 *    - OAuth2, JWT, LDAP 등 다양한 인증 제공자 구성
 *    - 다중 인증 제공자(Multi-provider) 체인 구성
 *    
 *    예시:
 *    @Bean
 *    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
 *        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
 *        auth.authenticationProvider(daoAuthenticationProvider())
 *            .authenticationProvider(jwtAuthenticationProvider())
 *            .authenticationProvider(oauth2AuthenticationProvider());
 *        return auth.build();
 *    }
 * 
 * 2. 비밀번호 인코딩 전략 고도화:
 *    - 더 강력한 해싱 알고리즘 사용 (Argon2, PBKDF2 등)
 *    - 비밀번호 강도 검증 로직 추가
 *    
 *    예시:
 *    @Bean
 *    public PasswordEncoder passwordEncoder() {
 *        return new Argon2PasswordEncoder(16, 32, 1, 16384, 2);
 *    }
 * 
 * 3. 커스텀 AuthenticationProvider 구현:
 *    - 특수한 인증 로직(예: 2FA, 생체인증)을 위한 커스텀 제공자 개발
 *    - 기존 제공자 확장으로 추가 검증 로직 구현
 * 
 * 4. 프로필 기반 빈 설정:
 *    - 개발, 테스트, 운영 환경별 다른 보안 설정 적용
 *    - @Profile 어노테이션을 활용한 환경별 빈 구성
 *    
 *    예시:
 *    @Bean
 *    @Profile("dev")
 *    public PasswordEncoder devPasswordEncoder() {
 *        return NoOpPasswordEncoder.getInstance(); // 개발 환경에서만 사용
 *    }
 * 
 * 5. 조건부 빈 구성:
 *    - @ConditionalOnProperty, @ConditionalOnClass 등을 활용한 유연한 빈 구성
 *    - 특정 조건에 따라 다른 인증 방식 활성화
 * 
 * 6. 감사 기능 강화:
 *    - 상세한 감사 이벤트 기록
 *    - 민감 작업에 대한 추적성 향상
 *    - 로그 통합 및 알림 시스템 연동
 * 
 * 7. 보안 지표 모니터링:
 *    - 인증 시도, 실패, 성공 통계 수집
 *    - Actuator와 Micrometer 통합으로 보안 메트릭 노출
 *    - 이상 징후 감지 시스템 구축
 */
