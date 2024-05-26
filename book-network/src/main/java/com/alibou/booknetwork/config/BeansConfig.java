package com.alibou.booknetwork.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class BeansConfig {
    private final UserDetailsService userDetailsService; // we dont want to use default implementation of UserDetailsService, we want to use our own implementation (UserDetailsServiceImpl)
    // we want to load user by our own

    @Bean
    public AuthenticationProvider authenticationProvider() { // if not created, we will get could not autowire error in SecurityConfig
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(); // DaoAuthenticationProvider is a concrete implementation of AuthenticationProvider
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
