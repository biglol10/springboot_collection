package com.biglol.getinline.service;

import com.biglol.getinline.constant.ErrorCode;
import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventDTO;
import com.biglol.getinline.exception.GeneralException;
import com.biglol.getinline.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {
    public List<EventDTO> getEvents(
            Long placeId,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime
    ) {
        return List.of();
    }

    public Optional<EventDTO> findEvent(Long eventId) {
        return Optional.empty();
    }

    public boolean createEvent(EventDTO eventDTO) {
        return true;
    }

    public boolean modifyEvent(Long eventId, EventDTO eventDTO) {
        return true;
    }

    public boolean removeEvent(Long eventId) {
        return true;
    }

}
