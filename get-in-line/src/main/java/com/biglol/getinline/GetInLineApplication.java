package com.biglol.getinline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan // https://developtrace.tistory.com/25
@SpringBootApplication
public class GetInLineApplication {

    public static void main(String[] args) {
        SpringApplication.run(GetInLineApplication.class, args);
    }
}
