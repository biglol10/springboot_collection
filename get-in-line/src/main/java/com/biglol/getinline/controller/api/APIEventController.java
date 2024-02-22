package com.biglol.getinline.controller.api;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.APIDataResponse;
import com.biglol.getinline.dto.EventRequest;
import com.biglol.getinline.dto.EventResponse;

@RequestMapping("/api")
@RestController
public class APIEventController {
    @GetMapping("/events")
    public APIDataResponse<List<EventResponse>> getEvents() {
        //        throw new GeneralException("테스트 메시지");
        //        throw new HttpRequestMethodNotSupportedException("asdf");
        //        return List.of("event1", "event2");

        return APIDataResponse.of(
                List.of(
                        EventResponse.of(
                                1L,
                                "오후 운동",
                                EventStatus.OPENED,
                                LocalDateTime.of(2021, 1, 1, 13, 0, 0),
                                LocalDateTime.of(2021, 1, 1, 16, 0, 0),
                                0,
                                24,
                                "마스크 꼭 착용하세요")));
    }

    //    @PostMapping("/events")
    //    public Boolean createEvent() {
    //        //        throw new RuntimeException("runtime 테스트 메시지"); // 이건 generalException에서 잡지
    // 못한 에러니
    //        // 공통에러로 넘어감
    //        //        throw new GeneralException("테스트");
    //        return true;
    //    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/events")
    public APIDataResponse<Void> createEvent(@RequestBody EventRequest eventRequest) {
        return APIDataResponse.empty();
    }

    @GetMapping("/events/{eventId}")
    public APIDataResponse<EventResponse> getEvent(@PathVariable Long eventId) {
        if (eventId.equals(2L)) {
            return APIDataResponse.empty();
        }

        return APIDataResponse.of(
                EventResponse.of(
                        1L,
                        "오후 운동",
                        EventStatus.OPENED,
                        LocalDateTime.of(2021, 1, 1, 13, 0, 0),
                        LocalDateTime.of(2021, 1, 1, 16, 0, 0),
                        0,
                        24,
                        "마스크 꼭 착용하세요"));
    }

    @PutMapping("/events/{eventId}")
    public APIDataResponse<Void> modifyEvent(
            @PathVariable Long eventId, @RequestBody EventRequest eventRequest) {
        return APIDataResponse.empty();
    }

    @DeleteMapping("/events/{eventId}")
    public APIDataResponse<Void> removeEvent(@PathVariable Long eventId) {
        return APIDataResponse.empty();
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
