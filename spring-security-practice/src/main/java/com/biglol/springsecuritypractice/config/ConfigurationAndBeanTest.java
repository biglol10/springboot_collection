package com.biglol.springsecuritypractice.config;

import com.biglol.springsecuritypractice.user.User;
import com.biglol.springsecuritypractice.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // 이거 없이 그냥 @Bean 등록하면 적용안됨 (user2라는 사람이 등록되지 않음)
@RequiredArgsConstructor
public class ConfigurationAndBeanTest {
    private final UserService userService;

    @Bean
    public void init() {
        User user = userService.signup("user2", "user2");
    }
}
