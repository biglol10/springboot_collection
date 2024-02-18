package com.fastcampus.springbootpractice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor // Jackson을 쓰게 되면 해당 annotation이 reflection을 이용해서 해당 클래스를 읽게 됨.
// 그래서 기본 생성자가 필요한데 AllArgsConstructor로 전체 생성자 하나 만들고 그것도 지워버림. staticName을 쓰면 생성자가 private로 바뀌어버림. 그래서 빈을 찾지 못하게 되서 redis에서 오류가 발생
@AllArgsConstructor(staticName = "of")
@Data
public class  Student {
    private String name;
    private Integer age;
    private Grade grade;

    public enum Grade {
        A, B, C, D, F
    }
}
