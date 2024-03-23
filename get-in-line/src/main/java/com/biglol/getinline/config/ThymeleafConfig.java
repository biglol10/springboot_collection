package com.biglol.getinline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Configuration
public class ThymeleafConfig {
    @Bean
    public SpringResourceTemplateResolver thymeleafTemplateResolver(
            SpringResourceTemplateResolver defaultTemplateResolver,
            Thymeleaf3Properties thymeleaf3Properties) {
        defaultTemplateResolver.setUseDecoupledLogic(thymeleaf3Properties.isDecoupledLogic());

        return defaultTemplateResolver;
    }

    @Getter
    @RequiredArgsConstructor
    @ConstructorBinding
    @ConfigurationProperties("spring.thymeleaf3")
    public static class Thymeleaf3Properties {
        /** Thymeleaf 3 Decoupled Logic 활성화 */
        private final boolean decoupledLogic;
    }

    //    @Bean
    //    public SpringResourceTemplateResolver thymeleafTemplateResolver(
    //            SpringResourceTemplateResolver defaultTemplateResolver,
    //            Thymeleaf3Properties thymeleaf3Properties) {
    //        defaultTemplateResolver.setUseDecoupledLogic(thymeleaf3Properties.isDecoupledLogic());
    //
    //        return defaultTemplateResolver;
    //    }
    //
    //    // https://developtrace.tistory.com/25
    //    // index.html, index.th.xml 의 데이터 매핑을 위한거임
    //    @Getter
    //    @RequiredArgsConstructor
    //    //    @ConstructorBinding
    //    @ConfigurationProperties("spring.thymeleaf3")
    //    public static class Thymeleaf3Properties {
    //        /** Thymeleaf 3 Decoupled Logic 활성화 */
    //        private final boolean decoupledLogic;
    //    }
}
