package com.biglol.getinline.service;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class EventServiceTest {
    private EventService sut; // system under test

    @DisplayName("검색 조건 없이 이벤트 검색하면, 전체 검색 결과를 출력하여 보여준다")
    @Test
    void giveNothing_whenSearchingEvents_thenReturnEntireEventList() {
        // GIVEN


        // WHEN
        List<EventDTO> list = sut.getEvents(null, null, null, null, null);

        // THEN
        assertThat(list).hasSize(2);
    }

    @DisplayName("검색과 함께 이벤트 검색하면, 검색 결과를 출력하여 보여준다")
    @Test
    void giveSearchParams_whenSearchingEvents_thenReturnEventList() {
        // GIVEN
        Long placeId = 1L;
        String eventName = "오전 운동";
        EventStatus eventStatus = EventStatus.OPENED;
        LocalDateTime eventStartDatetime = LocalDateTime.of(2021, 1, 1, 13, 0, 0);
        LocalDateTime eventEndDatetime = LocalDateTime.of(2021, 1, 1, 16, 0, 0);

        // WHEN
        List<EventDTO> list = sut.getEvents(placeId, eventName, eventStatus, eventStartDatetime, eventEndDatetime);

        // THEN
        assertThat(list).hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event)
                            .hasFieldOrPropertyWithValue("placeId", placeId)
                            .hasFieldOrPropertyWithValue("eventName", eventName)
                            .hasFieldOrPropertyWithValue("eventStatus", eventStatus);
//                    assertThat(event.eventStartDatetime().isAfter(eventStartDatetime));
                });  // 조건문을 적는데 그 조건을 모두 만족시키는지 검사하는 테스트 작성
    }
}