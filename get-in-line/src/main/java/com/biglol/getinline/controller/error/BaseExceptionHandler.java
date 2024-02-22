package com.biglol.getinline.controller.error;

import java.util.Map;

import org.springframework.http.HttpStatus;
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
        HttpStatus status =
                errorCode.isClientSideError()
                        ? HttpStatus.BAD_REQUEST
                        : HttpStatus.INTERNAL_SERVER_ERROR;

        return new ModelAndView(
                "error",
                Map.of(
                        "statusCode",
                        status.value(),
                        "errorCode",
                        errorCode,
                        "message",
                        errorCode.getMessage(e)), // 이 내용이 먼저 우선 하고 이 내용이 없으면 기본 error을 출력
                status);
    }

    // 전체적으로 에러 터졌을 때의 케이스
    @ExceptionHandler
    public ModelAndView exception(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        return new ModelAndView(
                "error",
                Map.of(
                        "statusCode", status.value(),
                        "errorCode", errorCode,
                        "message", errorCode.getMessage(e) // 이 내용이 먼저 우선 하고 이 내용이 없으면 기본 error을 출력
                        ),
                status);
    }
}
