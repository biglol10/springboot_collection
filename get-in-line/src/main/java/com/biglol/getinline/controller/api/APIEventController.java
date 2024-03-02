package com.biglol.getinline.controller.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.APIDataResponse;
import com.biglol.getinline.dto.EventRequest;
import com.biglol.getinline.dto.EventResponse;
import com.biglol.getinline.service.EventService;

import lombok.RequiredArgsConstructor;

/**
 * Spring Data REST 로 API 를 만들어서 당장 필요가 없어진 컨트롤러. 우선 deprecated 하고, 향후 사용 방안을 고민해 본다. 필요에 따라서는 다시 살릴
 * 수도 있음
 *
 * @deprecated 0.1.2
 */
@Deprecated
@RequiredArgsConstructor
// @Validated
// @RequestMapping("/api")
// @RestController
public class APIEventController {
    private final EventService eventService;

    @GetMapping("/events")
    public APIDataResponse<List<EventResponse>> getEvents(
            @Positive Long placeId,
            @Size(min = 2) String eventName,
            EventStatus eventStatus,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime
                            eventStartDatetime, // String 문자열로 들어간 query parameter을 local datetime으로
            // 변환
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime eventEndDatetime) {
        //        throw new GeneralException("테스트 메시지");
        //        throw new HttpRequestMethodNotSupportedException("asdf");
        //        return List.of("event1", "event2");

        //        return APIDataResponse.of(
        //                List.of(
        //                        EventResponse.of(
        //                                1L,
        //                                "오후 운동",
        //                                EventStatus.OPENED,
        //                                LocalDateTime.of(2021, 1, 1, 13, 0, 0),
        //                                LocalDateTime.of(2021, 1, 1, 16, 0, 0),
        //                                0,
        //                                24,
        //                                "마스크 꼭 착용하세요")));

        List<EventResponse> eventResponses =
                eventService.getEvents(null, null, null, null, null).stream()
                        .map(EventResponse::from)
                        .collect(Collectors.toList());
        return APIDataResponse.of(eventResponses);
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
    public APIDataResponse<String> createEvent(@Valid @RequestBody EventRequest eventRequest) {
        boolean result = eventService.createEvent(eventRequest.toDTO());

        return APIDataResponse.of(Boolean.toString(result));
    }

    @GetMapping("/events/{eventId}")
    public APIDataResponse<EventResponse> getEvent(@Positive @PathVariable Long eventId) {
        EventResponse eventResponse =
                EventResponse.from(eventService.getEvent(eventId).orElse(null));

        return APIDataResponse.of(eventResponse);
    }

    @PutMapping("/events/{eventId}")
    public APIDataResponse<String> modifyEvent(
            @Positive @PathVariable Long eventId, @Valid @RequestBody EventRequest eventRequest) {
        boolean result = eventService.modifyEvent(eventId, eventRequest.toDTO());
        return APIDataResponse.of(Boolean.toString(result));
    }

    @DeleteMapping("/events/{eventId}")
    public APIDataResponse<String> removeEvent(@Positive @PathVariable Long eventId) {
        boolean result = eventService.removeEvent(eventId);

        return APIDataResponse.of(Boolean.toString(result));
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
