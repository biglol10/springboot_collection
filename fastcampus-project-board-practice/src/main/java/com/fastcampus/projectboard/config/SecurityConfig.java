package com.fastcampus.projectboard.config;

//지금은 테스트 개발 단계고,
//아직 인증 기능 구현 단계에 이르지 않았으므로,
//모든 인증 설정을 오픈한다.
//그리고 폼 로그인을 활성화해서 로그인 뷰가 그려지게끔 함
//최신 스프링 부트 2.7 의 시큐리티 설정 방법을 활용함
//
//* https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

//@EnableWebSecurity // 이거 사용하지 않아도 되는 이유는 https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter
// spring boot에서 spring security를 연동해서 쓸 때는 autoconfiguration이 되어서
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin().and()
                .build();
    }
}
