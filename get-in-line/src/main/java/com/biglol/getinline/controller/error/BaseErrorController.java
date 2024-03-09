package com.biglol.getinline.controller.error;

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.biglol.getinline.constant.ErrorCode;
import com.biglol.getinline.dto.ApiErrorResponse;

@Controller
public class BaseErrorController implements ErrorController { // 에러가 전파됨 (ErrorController을 타고 넘어옴)
    @RequestMapping(path = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView errorHtml(HttpServletResponse response) {
        HttpStatus status = HttpStatus.valueOf(response.getStatus());
        ErrorCode errorCode =
                status.is4xxClientError() ? ErrorCode.BAD_REQUEST : ErrorCode.INTERNAL_ERROR;

        return new ModelAndView(
                "error",
                Map.of(
                        "statusCode", status.value(),
                        "errorCode", errorCode,
                        "message", errorCode.getMessage(status.getReasonPhrase())),
                status);
    }

    @RequestMapping("/error")
    public ResponseEntity<ApiErrorResponse> error(HttpServletResponse response) {
        HttpStatus status = HttpStatus.valueOf(response.getStatus());
        ErrorCode errorCode =
                status.is4xxClientError() ? ErrorCode.BAD_REQUEST : ErrorCode.INTERNAL_ERROR;

        return ResponseEntity.status(status).body(ApiErrorResponse.of(false, errorCode));
    }

    //    @RequestMapping(
    //            path = "/error",
    //            produces =
    //                    MediaType.TEXT_HTML_VALUE) // 이쪽에서는 view로 잡음, 그리고 text-html을 accept
    // header로 가지고
    //    // 있는 녀석만 잡으니까 view가 view에만 해당하게 될 것임
    //    public ModelAndView errorHtml(HttpServletResponse response) {
    //        HttpStatus status = HttpStatus.valueOf(response.getStatus()); // 숫자로부터 HttpStatus객체를
    // 만듦
    //        ErrorCode errorCode =
    //                status.is4xxClientError() ? ErrorCode.BAD_REQUEST : ErrorCode.INTERNAL_ERROR;
    //        return new ModelAndView(
    //                "error",
    //                Map.of(
    //                        "statusCode",
    //                        status.value(),
    //                        "errorCode",
    //                        errorCode,
    //                        "message",
    //                        errorCode.getMessage(
    //                                status.getReasonPhrase())), // 이 내용이 먼저 우선 하고 이 내용이 없으면 기본
    // error을 출력
    //                status);
    //    }

    //    @RequestMapping("/error") // 이쪽에서는 json-body로 잡음
    //    public ResponseEntity<APIErrorResponse> error(HttpServletResponse response) {
    //        HttpStatus status = HttpStatus.valueOf(response.getStatus()); // 숫자로부터 HttpStatus객체를
    // 만듦
    //        ErrorCode errorCode =
    //                status.is4xxClientError() ? ErrorCode.BAD_REQUEST : ErrorCode.INTERNAL_ERROR;
    //
    //        return ResponseEntity.status(status).body(APIErrorResponse.of(false, errorCode));
    //    }
}
