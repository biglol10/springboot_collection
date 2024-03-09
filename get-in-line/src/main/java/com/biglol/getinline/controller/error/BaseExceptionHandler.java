package com.biglol.getinline.controller.error;

import java.util.Map;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.biglol.getinline.constant.ErrorCode;
import com.biglol.getinline.exception.GeneralException;

// 여기에선 뷰에 대한 ControllerAdvice를 만듦
@ControllerAdvice // 전체 컨트롤러의 동작을 감시
public class BaseExceptionHandler {
    // general용
    @ExceptionHandler
    public ModelAndView general(GeneralException e) {
        ErrorCode errorCode = e.getErrorCode();

        return new ModelAndView(
                "error",
                Map.of(
                        "statusCode", errorCode.getHttpStatus().value(),
                        "errorCode", errorCode,
                        "message", errorCode.getMessage()),
                errorCode.getHttpStatus());
    }

    @ExceptionHandler
    public ModelAndView exception(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;

        return new ModelAndView(
                "error",
                Map.of(
                        "statusCode", errorCode.getHttpStatus().value(),
                        "errorCode", errorCode,
                        "message", errorCode.getMessage(e)),
                errorCode.getHttpStatus());
    }
}
