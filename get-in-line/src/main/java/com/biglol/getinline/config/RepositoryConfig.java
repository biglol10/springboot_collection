package com.biglol.getinline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.biglol.getinline.repository.EventRepository;

@Configuration
public class RepositoryConfig {
    @Bean // 구현체를 bin으로 등록할 수 있는 방법을 스프링에서 가르쳐준거임
    public EventRepository eventRepository() {
        return new EventRepository() {};
    }
}
