package com.biglol.getinline.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.biglol.getinline.constant.ErrorCode;
import com.biglol.getinline.dto.APIErrorResponse;
import com.biglol.getinline.exception.GeneralException;

@RequestMapping("/api")
@RestController
public class APIEventController {
    @GetMapping("/events")
    public List<String> getEvents() {
        throw new GeneralException("테스트 메시지");
        //        return List.of("event1", "event2");
    }

    @PostMapping("/events")
    public Boolean createEvent() {
        throw new RuntimeException("runtime 테스트 메시지"); // 이건 generalException에서 잡지 못한 에러니 공통에러로 넘어감
        //        return true;
    }

    @GetMapping("/events/{eventId}")
    public String getEvent(@PathVariable Integer eventId) {
        return "event " + eventId;
    }

    @PutMapping("/events/{eventId}")
    public Boolean modifyEvent(@PathVariable Integer eventId) {
        return true;
    }

    @DeleteMapping("/events/{eventId}")
    public Boolean removeEvent(@PathVariable Integer eventId) {
        return true;
    }

    @ExceptionHandler // 이 ExceptionHandler는 이 컨트롤러에 있는 handler method중에서 generalException이 터지는걸
    // 잡아냄. 범위가 여기로 한정됨
    public ResponseEntity<APIErrorResponse> general(GeneralException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status =
                errorCode.isClientSideError()
                        ? HttpStatus.BAD_REQUEST
                        : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(APIErrorResponse.of(false, errorCode, errorCode.getMessage(e)));
    }
}
