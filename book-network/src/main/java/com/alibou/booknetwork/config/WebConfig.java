package com.alibou.booknetwork.config;

import com.alibou.booknetwork.handler.RoleCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 이 설정을 통해 '/admin/**' 경로로 시작하는 모든 요청은 RoleCheckInterceptor를 거치게 되며, 'ADMIN' 역할을 가지고 있지 않은 사용자는 접근이 거부됩니다

@Configuration
public class WebConfig implements WebMvcConfigurer {

//    private final RoleCheckInterceptor roleCheckInterceptor;
//
//    public WebConfig(RoleCheckInterceptor roleCheckInterceptor) {
//        this.roleCheckInterceptor = roleCheckInterceptor;
//    }
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(roleCheckInterceptor).addPathPatterns("/admin/**");
//    }
}
