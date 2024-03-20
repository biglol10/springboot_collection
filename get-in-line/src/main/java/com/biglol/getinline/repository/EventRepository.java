package com.biglol.getinline.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.domain.Event;
import com.biglol.getinline.domain.Place;
import com.biglol.getinline.domain.QEvent;
import com.biglol.getinline.repository.querydsl.EventRepositoryCustom;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.StringExpression;

// QuerydslPredicateExecutor는 모든 필드들을 부분적으로 검색할 수 있게 해줌
// 구현체 안 만듦. JpaRepository가 해줌. Spring Data JPA가 Simple JPA 리포지토리를 주입해줌
public interface EventRepository
        extends JpaRepository<
                        Event,
                        Long>, // JpaRepository대신 EventReadOnlyRepository로 변경 가능 (조회 기능만 넣고 싶으면)
                EventRepositoryCustom, // 넣어주면 자연스레 연동됨
                QuerydslPredicateExecutor<Event>,
                QuerydslBinderCustomizer<QEvent> {
    //    @Query("select e from Event e where eventName = :eventName and eventStatus =
    // :eventStatus")
    List<Event> findByEventNameAndEventStatus(
            String eventName,
            EventStatus eventStatus); // 쿼리 메소드. dynamic query는 안됨. 값을 안 넣었을 때 전체검색과 같은 행위 안됨

    // 안 넣으면 is null로 됨. join도 안됨. 이건 순서 기반으로 되는데 그게 싫으면 @Param("eventName") String eventName하면 됨
    Optional<Event> findFirstByEventEndDatetimeBetween(LocalDateTime from, LocalDateTime to);

    // 이벤트에서 api호출해서 이벤트 조회할 때 검색어들을 이 안에 넣으려고 했음
    // 연관관계를 맺으면서 place name으로 조회하는게 가능해짐. 그래서 place.placeName으로 넣음
    // QuerydslPredicateExecutor랑 QuerydslBinderCustomizer를 이용해서 검색 쿼리를 자동으로 만들어줌
    @Override
    default void customize(QuerydslBindings bindings, QEvent root) {
        bindings.excludeUnlistedProperties(true);
        bindings.including( // 검색할 것들 지정
                root.place.placeName,
                root.eventName,
                root.eventStatus,
                root.eventStartDatetime,
                root.eventEndDatetime);
        bindings.bind(root.place.placeName)
                .as("placeName")
                .first(StringExpression::containsIgnoreCase); // placeName만 넣고도 검색이 되게끔
        bindings.bind(root.eventName).first(StringExpression::containsIgnoreCase);
        bindings.bind(root.eventStartDatetime).first(ComparableExpression::goe);
        bindings.bind(root.eventEndDatetime).first(ComparableExpression::loe);
    }

    Page<Event> findByPlace(Place place, Pageable pageable);
}

//// TODO: 인스턴스 설정 관리를 위해 임시로 default 사용. repository layer 구현이 완성되면 삭제
//// default를 해두면 RepositoryConfig에서 eventRepository 만들 때 익명 클래스 만들 때 이 안의 내부 구현을 원래 다 해줘야 됨. 그런데
//// default로 미리 다 넣어놨으니 그런 부분없이 깔끔하게 되는 꼼수 같은거임
// public interface EventRepository {
//    default List<EventDTO> findEvents(
//            Long placeId,
//            String eventName,
//            EventStatus eventStatus,
//            LocalDateTime eventStartDatetime,
//            LocalDateTime eventEndDatetime) {
//        return List.of();
//    }
//
//    default Optional<EventDTO> findEvent(Long eventId) {
//        return Optional.empty();
//    }
//
//    default boolean insertEvent(EventDTO eventDTO) {
//        return false;
//    }
//
//    default boolean updateEvent(Long eventId, EventDTO dto) {
//        return false;
//    }
//
//    default boolean deleteEvent(Long eventId) {
//        return false;
//    }
// }
