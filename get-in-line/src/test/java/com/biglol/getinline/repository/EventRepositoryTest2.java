package com.biglol.getinline.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.constant.PlaceType;
import com.biglol.getinline.domain.Event;
import com.biglol.getinline.domain.Place;
import com.querydsl.core.BooleanBuilder;

@Deprecated
@DataJpaTest // 이미 Transactional을 가지고 있음. 내용이 끝나면 롤백이 자동으로 일어나게 됨
class EventRepositoryTest2 {
    private final EventRepository sut;
    private final TestEntityManager testEntityManager;

    public EventRepositoryTest2(
            @Autowired EventRepository sut, @Autowired TestEntityManager testEntityManager) {
        this.sut = sut;
        this.testEntityManager = testEntityManager;
    }

    @DisplayName("test")
    @Test
    void test() {
        // Given
        Place place = createPlace();
        Event event = createEvent(place);
        testEntityManager.persist(place); // 테스트 데이터를 data.sql로 넣었으니 사실 entitymanager사용할 필요가 없음
        testEntityManager.persist(event);

        // When
        Iterable<Event> events =
                sut.findAll(new BooleanBuilder()); // Predicate이 interface라서 그대로 넣어줄 수 없고 Predicate을
        // 구현하는 걸로 BooleanBuilder사용. true false로 where 조건의
        // 결과를 리턴하는 건데 where 절의 결과가 true다 false다 이걸 만들 때
        // BooleanBuilder사용. 지금은 검색어를 넣지 않으니 가능

        // Then
        assertThat(events).hasSize(7); // 7개가 되고 마지막은 자동 롤백
    }

    private Event createEvent(Place place) {
        return createEvent(
                //                1L,
                //                1L,
                place, "test event", EventStatus.ABORTED, LocalDateTime.now(), LocalDateTime.now());
    }

    private Event createEvent(
            //            long id,
            //            long placeId,
            Place place,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDateTime,
            LocalDateTime eventEndDateTime) {
        Event event =
                Event.of(
                        place,
                        eventName,
                        eventStatus,
                        eventStartDateTime,
                        eventEndDateTime,
                        0,
                        24,
                        "마스크 꼭 착용하세요");
        //        ReflectionTestUtils.setField(event, "id", id);

        return event;
    }

    private Place createPlace() {
        Place place =
                Place.of(PlaceType.COMMON, "test place", "test address", "010-1234-1234", 10, null);
        //        ReflectionTestUtils.setField(place, "id", 1L); // ReflectionTestUtils는 테스트 도구 중에
        // 지원해주는 거임. test중에 쓰고 reflection을 도와줌
        // entity안에 id는 setter을 가지고 있지 않음. 영속성 context를 사용하지 않고 테스트 해야 할 때 저 부분이 null인 상태로 테스트를 해야되는
        // 문제가 발생.
        // 그래서 ReflectionTestUtils로 강제로 private field를 찾아서 데이터를 넣어줌
        // 영속성 컨텍스트에 넣을거라 주석처리

        return place;
    }
}
