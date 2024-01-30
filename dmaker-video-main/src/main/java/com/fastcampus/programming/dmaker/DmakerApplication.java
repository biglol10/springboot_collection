package com.fastcampus.programming.dmaker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

//@EnableJpaAuditing  // https://velog.io/@sysy123/Spring-Boot-WebMvcTest-Junit5-Error-JPA-metamodel-must-not-be-empty
@SpringBootApplication
public class DmakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DmakerApplication.class, args);
    }

}
