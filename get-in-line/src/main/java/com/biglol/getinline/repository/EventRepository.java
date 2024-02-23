package com.biglol.getinline.repository;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventDTO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository {
    default List<EventDTO> findEvents(
            Long placeId,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime
    ) { return List.of(); }
    default Optional<EventDTO> findEvent(Long eventId) { return Optional.empty(); }
    default boolean insertEvent(EventDTO eventDTO) { return false; }
    default boolean updateEvent(Long eventId, EventDTO dto) { return false; }
    default boolean deleteEvent(Long eventId) { return false; }
}
