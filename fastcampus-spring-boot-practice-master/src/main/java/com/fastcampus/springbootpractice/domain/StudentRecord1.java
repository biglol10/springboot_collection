package com.fastcampus.springbootpractice.domain;

// StudentRecord1 fred1 = new StudentRecord1("fred", 21, StudentRecord1.Grade.A);
public record StudentRecord1(String name, Integer age, Student.Grade grade) {
    public static StudentRecord1 of(String name, Integer age, Student.Grade grade) {
        return new StudentRecord1(name, age, grade);
    }

    public enum Grade {
        A, B, C, D, E
    }
}
