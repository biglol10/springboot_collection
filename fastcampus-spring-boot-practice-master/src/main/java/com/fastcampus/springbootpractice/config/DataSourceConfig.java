package com.fastcampus.springbootpractice.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

// DataSource bean을 만듦으로써 db에 접속할 수 있는 정보를 spring boot에 알려줌
// ! 그러나 이게 application.properties에도 되니 안해도 됨 (또는 application.yml)

@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        builder.username("asdf");
        builder.password("asdfasdf");
        builder.url("jdbc:h2:mem:test");
        builder.driverClassName("org.h2.Driver");

        return builder.build();
    }
}
