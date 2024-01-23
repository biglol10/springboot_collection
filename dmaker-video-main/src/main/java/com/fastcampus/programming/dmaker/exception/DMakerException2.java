package com.fastcampus.programming.dmaker.exception;

import lombok.Getter;

@Getter
public class DMakerException2 extends RuntimeException {
    private DMakerErrorCode dMakerErrorCode;
    private String detailMessage;

    public DMakerException2(DMakerErrorCode errorCode) {
        super(errorCode.getMessage());
        this.dMakerErrorCode = errorCode;
        this.detailMessage = errorCode.getMessage();
    }

    public DMakerException2(DMakerErrorCode errorCode, String detailMessage){
        super(detailMessage);
        this.dMakerErrorCode = errorCode;
        this.detailMessage = detailMessage;
    }
}
