package com.biglol.getinline.controller.error;

import javax.validation.ConstraintViolationException;

import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.biglol.getinline.constant.ErrorCode;
import com.biglol.getinline.dto.ApiErrorResponse;
import com.biglol.getinline.exception.GeneralException;

@RestControllerAdvice(annotations = {RestController.class, RepositoryRestController.class})
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
        return handleExceptionInternal(e, ErrorCode.VALIDATION_ERROR, request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> general(GeneralException e, WebRequest request) {
        return handleExceptionInternal(e, e.getErrorCode(), request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        return handleExceptionInternal(e, ErrorCode.INTERNAL_ERROR, request);
    }

    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return handleExceptionInternal(ex, ErrorCode.valueOf(status), headers, status, request);
    }

    private ResponseEntity<Object> handleExceptionInternal(
            Exception e, ErrorCode errorCode, WebRequest request) {
        return handleExceptionInternal(
                e, errorCode, HttpHeaders.EMPTY, errorCode.getHttpStatus(), request);
    }

    private ResponseEntity<Object> handleExceptionInternal(
            Exception e,
            ErrorCode errorCode,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        return super.handleExceptionInternal(
                e,
                ApiErrorResponse.of(false, errorCode.getCode(), errorCode.getMessage(e)),
                headers,
                status,
                request);
    }
}
