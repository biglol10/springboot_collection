package com.biglol.getinline.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.biglol.getinline.constant.ErrorCode;
import com.biglol.getinline.dto.APIErrorResponse;
import com.biglol.getinline.exception.GeneralException;

// 여기에선 api에 대한 ControllerAdvice를 만듦
@RestControllerAdvice(
        annotations =
                RestController
                        .class) // 전체 컨트롤러의 동작을 감시. 여기에선 모든 응답은 response-body annotation이 추가로 붙게 됨.
// 얘가 잡는 대상은 전체 범위가 아니라 RestController annotation을 쓰는 것들만을 대상으로 좁혀
// 놓음. View들은 이 에러 핸들러에 영향을 받지 않게 됨
public class ApiExceptionHandler {
    // general용
    @ExceptionHandler
    public ResponseEntity<APIErrorResponse> general(GeneralException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status =
                errorCode.isClientSideError()
                        ? HttpStatus.BAD_REQUEST
                        : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(APIErrorResponse.of(false, errorCode, errorCode.getMessage(e)));
    }

    @ExceptionHandler
    public ResponseEntity<APIErrorResponse> exception(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(APIErrorResponse.of(false, errorCode, errorCode.getMessage(e)));
    }
}
