package com.fastcampus.springbootpractice.domain;

// 불편객체
// record를 쓰면 외부의존성 없이 깔끔하게 만들 수 있음
// StudentRecord fred1 = new StudentRecord("Fred", 21, StudentRecord.Grade.A);
// println("My name is " + fred1.name());
public record StudentRecord (
        String name,
        Integer age,
        Student.Grade grade
) {
    private enum Grade {
        A, B, C, D, E
    }
}
