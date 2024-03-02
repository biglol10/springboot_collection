package com.biglol.getinline.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventDto;

class EventServiceTest2 {
    private EventService sut; // system under test

    //    @BeforeEach // sut 구현체가 없는 상태라 임시로 이렇게 생성. 아니면 @SpringBootTest, @Autowired로 가능함. 그러나 이 방법은
    // container가 뜨는데 시간이 좀 걸려서 이렇게 생성자로 일단 씀
    //    void setUp() {
    //        sut = new EventService();
    //    }

    @DisplayName("검색 조건 없이 이벤트 검색하면, 전체 검색 결과를 출력하여 보여준다")
    @Test
    void giveNothing_whenSearchingEvents_thenReturnEntireEventList() {
        // GIVEN

        // WHEN
        List<EventDto> list = sut.getEvents(null, null, null, null, null);

        // THEN
        assertThat(list).hasSize(2);
    }

    @DisplayName("검색과 함께 이벤트 검색하면, 검색 결과를 출력하여 보여준다")
    @Test
    void giveSearchParams_whenSearchingEvents_thenReturnEvent() {
        // GIVEN
        Long eventId = 1L;
        String eventName = "오전 운동";
        EventStatus eventStatus = EventStatus.OPENED;
        LocalDateTime eventStartDatetime = LocalDateTime.of(2021, 1, 1, 13, 0, 0);
        LocalDateTime eventEndDatetime = LocalDateTime.of(2021, 1, 1, 16, 0, 0);

        // WHEN
        List<EventDto> list =
                sut.getEvents(
                        eventId, eventName, eventStatus, eventStartDatetime, eventEndDatetime);

        // THEN
        assertThat(list)
                .hasSize(1)
                .allSatisfy(
                        event -> {
                            assertThat(event)
                                    .hasFieldOrPropertyWithValue("placeId", eventId)
                                    .hasFieldOrPropertyWithValue("eventName", eventName)
                                    .hasFieldOrPropertyWithValue("eventStatus", eventStatus);
                            //
                            // assertThat(event.eventStartDatetime().isAfter(eventStartDatetime));
                        }); // 조건문을 적는데 그 조건을 모두 만족시키는지 검사하는 테스트 작성
    }

    @DisplayName("이벤트 ID로 존재하는 이벤트를 조회하면, 해당 이벤트 정보를 출력하여 보여준다")
    @Test
    void givenEventId_whenSearchingExistingEvent_thenReturnsEvent() {
        // GIVEN
        long eventId = 1L;
        EventDto eventDTO = createEventDTO(eventId, "오전 운동", true);

        // WHEN
        Optional<EventDto> result = sut.getEvent(eventId);

        // THEN
        assertThat(result).isEmpty();
    }

    @DisplayName("이벤트 ID의 정보를 주면, 이벤트 정보를 변경하고 결과를 true 로 보여준다")
    @Test
    void givenEventIdAndItsInfo_whenModifying_thenModifiesEventAndReturnsTrue() {
        long eventId = 1L;
        EventDto dto = createEventDTO(1L, "오후 운동", false);

        boolean result = sut.modifyEvent(eventId, dto);

        assertThat(result).isTrue();
    }

    @DisplayName("이벤트 ID를 주지 않으면, 이벤트 정보를 변경 중단하고 결과를 false로 보여준다")
    @Test
    void givenNoEventId_whenModifying_thenAbortModifyingAndReturnsFalse() {
        EventDto dto = createEventDTO(1L, "오후 운동", false);

        boolean result = sut.modifyEvent(null, dto);

        assertThat(result).isFalse();
    }

    @DisplayName("이벤트 ID를 주고 변경할 정보를 주지 않으면, 이벤트 정보를 변경 중단하고 결과를 false로 보여준다")
    @Test
    void givenEventIdOnly_whenModifying_thenAbortModifyingAndReturnsFalse() {
        long eventId = 1L;

        boolean result = sut.modifyEvent(eventId, null);

        assertThat(result).isFalse();
    }

    private EventDto createEventDTO(long placeId, String eventName, boolean isMorning) {
        String hourStart = isMorning ? "09" : "13";
        String hourEnd = isMorning ? "12" : "16";

        return createEventDTO(
                placeId,
                eventName,
                EventStatus.OPENED,
                LocalDateTime.parse("2021-01-01T%s:00:00".formatted(hourStart)),
                LocalDateTime.parse("2021-01-01T%s:00:00".formatted(hourEnd)));
    }

    private EventDto createEventDTO(
            long placeId,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDateTime,
            LocalDateTime eventEndDateTime) {
        return EventDto.of(
                placeId,
                eventName,
                eventStatus,
                eventStartDateTime,
                eventEndDateTime,
                0,
                24,
                "마스크 꼭 착용하세요",
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
