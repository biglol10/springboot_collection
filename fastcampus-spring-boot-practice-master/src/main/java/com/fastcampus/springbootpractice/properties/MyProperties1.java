package com.fastcampus.springbootpractice.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("my1")  // 첫번째 element 작성
public class MyProperties1 {
    private Integer myHeight; // my-height값 가져옴 (relaxed-binding-feature에 의해 변수명을 my-height, my_height도 가능함)
    private Integer height;


    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getMyHeight() {
        return myHeight;
    }

    public void setMyHeight(Integer myHeight) {
        this.myHeight = myHeight;
    }
}
