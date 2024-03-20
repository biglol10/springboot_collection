package com.biglol.getinline.repository.querydsl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventViewResponse;

public interface EventRepositoryCustom {
    Page<EventViewResponse> findEventViewPageBySearchParams(
            String placeName,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime,
            Pageable pageable // Pageable 인터페이스에서 페이징 정보를 여기서 추가로 마지막에 받게 됨
            );
}
