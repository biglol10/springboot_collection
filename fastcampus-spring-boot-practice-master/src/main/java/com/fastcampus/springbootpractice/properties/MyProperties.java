package com.fastcampus.springbootpractice.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@RequiredArgsConstructor
@ConstructorBinding
@ConfigurationProperties("my") // configuration property를 통해 application.properties에 관련 my.height쪽 설정 가능
public class MyProperties {

    /** 제 키에요. */
    private final Integer height;

}
