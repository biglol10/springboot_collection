package com.biglol.getinline.repository.querydsl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import com.biglol.getinline.constant.ErrorCode;
import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.domain.Event;
import com.biglol.getinline.domain.QEvent;
import com.biglol.getinline.dto.EventViewResponse;
import com.biglol.getinline.exception.GeneralException;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;

public class EventRepositoryCustomImpl extends QuerydslRepositorySupport
        implements EventRepositoryCustom {
    public EventRepositoryCustomImpl() {
        super(Event.class); // EventRepository이니 Event 엔터티를 넣어줌. QuerydslRepositorySupport에는
        // EntityManager가 이미 들어가있으니 EntityManager 셋업을 하지 않음
    }

    // insert, update, delete는 jpa꺼를 사용하고 보통 select만 querydsl사용
    @Override
    public Page<EventViewResponse> findEventViewPageBySearchParams(
            String placeName,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime,
            Pageable pageable) {
        QEvent event = QEvent.event;
        JPQLQuery<EventViewResponse> query =
                from(event) // JPQLQueryFactory방법은 Spring data jpa 쓰지 않았을 때.
                        .select(
                                Projections.constructor( // 그냥 Event를 가져올 거면 select(event) 또는 생략 가능.
                                        // 그런데 여기에선 placeName도 가져옴 (Custom Projection
                                        // 필요)
                                        EventViewResponse.class,
                                        event.event.id,
                                        event.place.placeName, // .join으로 해도 되지만 Place도
                                        // Entity이니 querydsl이 알아볼 수 있음
                                        // (그래서 sql log를 보면 join place가
                                        // 있음)
                                        event.eventName,
                                        event.eventStatus,
                                        event.eventStartDatetime,
                                        event.eventEndDatetime,
                                        event.currentNumberOfPeople,
                                        event.capacity,
                                        event.memo)); // select절 기본 뼈대 생성

        if (placeName != null && !placeName.isBlank()) {
            query.where(
                    event.place.placeName.containsIgnoreCase(
                            placeName)); // (검색을 원하는 것 + 검색하는 방법), containsIgnoreCase = 대소문자 구분 안함
        }
        if (eventName != null && !eventName.isBlank()) {
            query.where(event.eventName.containsIgnoreCase(eventName));
        }
        if (eventStatus != null) {
            query.where(event.eventStatus.eq(eventStatus));
        }
        if (eventStartDatetime != null) {
            query.where(event.eventStartDatetime.goe(eventStartDatetime));
        }
        if (eventEndDatetime != null) {
            query.where(event.eventEndDatetime.loe(eventEndDatetime));
        }

        List<EventViewResponse> events =
                Optional.ofNullable(getQuerydsl())
                        .orElseThrow(
                                () ->
                                        new GeneralException(
                                                ErrorCode.DATA_ACCESS_ERROR,
                                                "Spring Data JPA 로부터 Querydsl 인스턴스를 못 가져옴")) // getQuerydsl이 @Nullable이니 Optional로 감쌈
                        .applyPagination(pageable, query) // Pagable에 의해 페이징을 잘라놓은 그런 리스트가 됨
                        .fetch();

        return new PageImpl<>(
                events,
                pageable,
                query.fetchCount()); // query.fetchCount()넣지 않고 events.size()넣으면 안됨.
        // totalcount자리라 events.size()는 페이징에 의해 60개중 20개 이런식임
        // sql log를 보면 select query, count query 이렇게 2번 가져옴
    }
}
