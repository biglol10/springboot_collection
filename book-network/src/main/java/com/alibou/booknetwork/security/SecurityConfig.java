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

@Configuration // to mark a class as a configuration class
@EnableWebSecurity // to enable Spring Security’s web security support and provide the Spring MVC integration
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true) // since we spoke about role based authentication, we need to enable method security
public class SecurityConfig {
    private JwtFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    // When spring starts scanning the classes spring will find SecurityConfig annotated with @Configuration
    // then, it will find Beans that needs to be created and put into the spring context, it will scan every method annotated with @Bean
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeRequests(req ->
                            req.requestMatchers(
                                            "/auth/**",
                                            "/v2/api-docs",
                                            "/v3/api-docs",
                                            "/v3/api-docs/**",
                                            "/swagger-resources",
                                            "/swagger-resources/**",
                                            "/configuration/ui",
                                            "/configuration/security",
                                            "/swagger-ui/**",
                                            "/webjars/**",
                                            "/swagger-ui.html"
                            ).permitAll()
                                    .anyRequest().authenticated() // allow requests in requestMatchers and require authentication for any other request
                        )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // since we are using JWT, we don't need to create a session
                .authenticationProvider(authenticationProvider) // need to provide authenticationProvider object or bean
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // before UsernamePasswordAuthenticationFilter, we need to add jwtAuthFilter that checks the token
        return null;
    }
}
