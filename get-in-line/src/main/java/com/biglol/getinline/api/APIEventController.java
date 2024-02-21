package com.biglol.getinline.api;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RequestMapping("/api")
@RestController
public class APIEventController {
    @GetMapping("/events")
    public List<String> getEvents() {
        //        throw new GeneralException("테스트 메시지");
        //        throw new HttpRequestMethodNotSupportedException("asdf");
        return List.of("event1", "event2");
    }

    @PostMapping("/events")
    public Boolean createEvent() {
        //        throw new RuntimeException("runtime 테스트 메시지"); // 이건 generalException에서 잡지 못한 에러니
        // 공통에러로 넘어감
        //        throw new GeneralException("테스트");
        return true;
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

    //    @ExceptionHandler // 이 ExceptionHandler는 이 컨트롤러에 있는 handler method중에서 generalException이
    // 터지는걸
    //    // 잡아냄. 범위가 여기로 한정됨. 그러나 ExceptionHandler을 만들면 여기에서만 잡힘. 전역으로 놓을 수 있게끔
    // ApiExceptionHandler에서 RestControllerAdvice사용
    //    public ResponseEntity<APIErrorResponse> general(GeneralException e) {
    //        ErrorCode errorCode = e.getErrorCode();
    //        HttpStatus status =
    //                errorCode.isClientSideError()
    //                        ? HttpStatus.BAD_REQUEST
    //                        : HttpStatus.INTERNAL_SERVER_ERROR;
    //        return ResponseEntity.status(status)
    //                .body(APIErrorResponse.of(false, errorCode, errorCode.getMessage(e)));
    //    }
}
