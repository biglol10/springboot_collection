package com.alibou.booknetwork.discovery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Eureka 서버 보안 설정
 * 
 * 보안 정책:
 * 1. Eureka 대시보드: Basic 인증 필요
 * 2. 서비스 등록/해제: 인증 불필요 (내부 네트워크)
 * 3. 헬스체크 엔드포인트: 공개 접근
 * 4. 메트릭 엔드포인트: 모니터링 시스템용 공개
 * 
 * 운영환경 고려사항:
 * - 네트워크 레벨 보안: VPC, Security Group 설정
 * - TLS 암호화: HTTPS 인증서 적용
 * - 방화벽: 필요한 포트만 개방
 * - 감사 로깅: 접근 로그 기록
 */
@Configuration
@EnableWebSecurity
public class EurekaServerConfig {

    /**
     * HTTP 보안 설정
     * 
     * 접근 제어 정책:
     * - /eureka/** : 서비스 등록/발견 API (공개)
     * - /actuator/** : 모니터링 엔드포인트 (공개)
     * - /* : Eureka 대시보드 (인증 필요)
     * 
     * CSRF 비활성화:
     * - REST API 서버이므로 CSRF 보호 불필요
     * - 마이크로서비스 간 통신에서 세션 사용하지 않음
     * 
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain
     * @throws Exception 보안 설정 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // CSRF 비활성화
            .authorizeHttpRequests(authz -> authz
                // 🔓 서비스 등록/발견 API - 공개 접근
                .requestMatchers("/eureka/**").permitAll()
                
                // 📊 모니터링 엔드포인트 - 공개 접근 (내부 모니터링 시스템용)
                .requestMatchers("/actuator/**").permitAll()
                
                // 🎯 Eureka 대시보드 - 인증 필요
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic
                .realmName("Eureka Server") // 인증 영역 이름
            )
            .build();
    }
}