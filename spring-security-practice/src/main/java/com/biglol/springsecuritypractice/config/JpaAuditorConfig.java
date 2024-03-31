package com.biglol.springsecuritypractice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // 이걸로 JPA 활성화
public class JpaAuditorConfig {
}
