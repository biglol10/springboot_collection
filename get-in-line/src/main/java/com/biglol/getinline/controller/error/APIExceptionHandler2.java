package com.biglol.getinline.controller.error;

import javax.validation.ConstraintViolationException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.biglol.getinline.constant.ErrorCode;
import com.biglol.getinline.dto.ApiErrorResponse;
import com.biglol.getinline.exception.GeneralException;

// 여기에선 api에 대한 ControllerAdvice를 만듦
// @RestControllerAdvice(
//        annotations =
//                RestController
//                        .class) // 전체 컨트롤러의 동작을 감시. 여기에선 모든 응답은 response-body annotation이 추가로 붙게
// 됨.
// 얘가 잡는 대상은 전체 범위가 아니라 RestController annotation을 쓰는 것들만을 대상으로 좁혀
// 놓음. View들은 이 에러 핸들러에 영향을 받지 않게 됨
// 그러나 spring boot은 spring안에 있는 spring web mvc에서도 다양한 작업들이 일어나고 있고 그 안에서 다양한 spring boot이 지정해놓은 에러들이
// 나타남. 그걸 처리하지 않으면 공통 에러 페이지로 빠지면서 보여지게 됨 (스프링 웹에서 내보내는 에러 처리)
// 그래서 따로 처리를 해줌 (extends ResponseEntityExceptionHandler) 그러나 내부에 handleExceptionInternal 호출을 보면
// body를 전부 null로 보냄.
// handleExceptionInternal는 protected이니 여기에서 override해줌
public class APIExceptionHandler2 extends ResponseEntityExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        HttpStatus status = HttpStatus.BAD_REQUEST;

        return super.handleExceptionInternal(
                e,
                ApiErrorResponse.of(false, errorCode.getCode(), errorCode.getMessage(e)),
                HttpHeaders.EMPTY,
                status,
                request);
    }

    // general용
    // 원래 return ResponseEntity.status(status).body(APIErrorResponse.of(false, errorCode,
    // errorCode.getMessage(e))); 였지만
    // handleExceptionInternal을 쓰도록 해서 형식을 맞추기 위해 변경 (APIErrorResponse -> Object, return문)
    @ExceptionHandler
    public ResponseEntity<Object> general(
            GeneralException e, WebRequest request) { // WebRequest는 Exception handler라면 다 지원하는 인자임
        ErrorCode errorCode = e.getErrorCode();
        //        HttpStatus status =
        //                errorCode.isClientSideError()
        //                        ? HttpStatus.BAD_REQUEST
        //                        : HttpStatus.INTERNAL_SERVER_ERROR;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        //        return ResponseEntity.status(status)
        //                .body(APIErrorResponse.of(false, errorCode, errorCode.getMessage(e)));

        return super.handleExceptionInternal(
                e,
                ApiErrorResponse.of(false, errorCode.getCode(), errorCode.getMessage(e)),
                HttpHeaders.EMPTY, // 현재 header로 우리가 보내는게 없기에 EMPTY
                status,
                request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        //        return ResponseEntity.status(status)
        //                .body(APIErrorResponse.of(false, errorCode, errorCode.getMessage(e)));

        return super.handleExceptionInternal(
                e,
                ApiErrorResponse.of(false, errorCode.getCode(), errorCode.getMessage(e)),
                HttpHeaders.EMPTY,
                status,
                request);
    }

    //    // 여기 내용을 똑같이 채워주돼 body만 추가
    //    @Override
    //    protected ResponseEntity<Object> handleExceptionInternal(
    //            Exception ex,
    //            @Nullable Object body,
    //            HttpHeaders headers,
    //            HttpStatusCode status,
    //            WebRequest request) {
    //        ErrorCode errorCode =
    //                status.is4xxClientError()
    //                        ? ErrorCode.SPRING_BAD_REQUEST
    //                        : ErrorCode.SPRING_INTERNAL_ERROR;
    //
    //        // 원래 함수를 그대로 쓰기 위해
    //        return super.handleExceptionInternal(
    //                ex,
    //                ApiErrorResponse.of(false, errorCode.getCode(), errorCode.getMessage(ex)),
    //                headers,
    //                status,
    //                request);
    //
    //        // 이렇게 하면 handleExceptionInternal을 재구현했기 때문에 이 안에 있는 Abstract class, Response Entity,
    //        // Exception Handler의 각각 세부 동작들이 전부 다 영향을 받음. 안에서 handleExceptionInternal를 호출하면
    //        // 여기에서의 handleExceptionInternal를 호출하는 것과 같게 됨
    //    }
}
