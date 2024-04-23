package com.fastcampus.projectboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@EnableJpaAuditing
@Configuration // Configuration Bean이 되게끔. 각종 설정을 정의할 때
public class JpaConfig {
    @Bean
    public AuditorAware<String> auditorAware() { // id가 들어감. auditing을 할 때마다 여기에 해당되는 id가 들어감
        return () -> Optional.of("biglol"); // TODO: 스프링 시큐리티로 인증 기능을 붙이게 될 때 수정 필요
    }
}
