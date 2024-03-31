package com.biglol.springsecuritypractice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMVC Config
 */

// The provided MvcConfig class is a configuration class for Spring MVC. It implements the WebMvcConfigurer interface, which allows you to customize the behavior of Spring MVC.
// In this specific configuration, the addViewControllers() method is overridden to register some view controllers. These view controllers are mapped to specific URLs and are responsible for rendering the corresponding views.

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/home").setViewName("index"); //  This line adds a view controller for the "/home" URL mapping. When a request is made to "/home", Spring MVC will render the view named "index". This is typically used for mapping a specific URL to a view without any additional processing logic
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/login").setViewName("login");
    }
}
