package com.biglol.getinline.repository.querydsl;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventViewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

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
