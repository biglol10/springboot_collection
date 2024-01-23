package com.fastcampus.programming.dmaker.exception;

import lombok.Getter;

@Getter
public class DMakerException2 extends RuntimeException {
    private DMakerErrorCode dMakerErrorCode;
    private String detailMessage;
}
