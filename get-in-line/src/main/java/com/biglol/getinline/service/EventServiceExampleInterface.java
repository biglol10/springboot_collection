package com.biglol.getinline.service;

import java.time.LocalDateTime;
import java.util.List;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventDto;

/**
 * 이벤트 서비스
 *
 * @author biglol
 */
public interface EventServiceExampleInterface {
    /**
     * 검색어를 받아서 이벤트 리스트를 반환
     *
     * @param placeId 장소ID
     * @param eventName 이벤트 이름
     * @param eventStatus 이벤트 상태
     * @param eventStartDatetime 시작시간
     * @param eventEndDatetime 종료시간
     * @return
     */
    List<EventDto> findEvents(
            Long placeId,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime);
}
