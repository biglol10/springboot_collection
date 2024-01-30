package com.fastcampus.programming.dmaker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * @author Snow
 */
// https://velog.io/@sysy123/Spring-Boot-WebMvcTest-Junit5-Error-JPA-metamodel-must-not-be-empty
@Configuration
@EnableJpaAuditing // 이 bean이 뜰 때는 이 설정이 먹고 이 빈이 안 뜰 때는 auditing 기능이 따로 동작하지 않는 상태로
public class JpaConfig {
}
