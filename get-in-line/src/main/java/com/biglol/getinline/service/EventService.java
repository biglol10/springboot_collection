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

@RequiredArgsConstructor
@Service
public class EventService {

    private final EventRepository eventRepository;

    public List<EventDTO> getEvents(
            Long placeId,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime
    ) {
        return List.of(EventDTO.of(
                1L,
                "오후 운동",
                EventStatus.OPENED,
                LocalDateTime.parse("2021-01-01T00:00:00"),
                LocalDateTime.parse("2021-02-01T00:00:00"),
                0,
                24,
                "마스크 꼭 착용하세요",
                LocalDateTime.now(),
                LocalDateTime.now()
        ),
                EventDTO.of(
                        1L,
                        "오후 운동2",
                        EventStatus.OPENED,
                        LocalDateTime.parse("2021-03-01T00:00:00"),
                        LocalDateTime.parse("2021-04-01T00:00:00"),
                        0,
                        24,
                        "마스크 꼭 착용하세요",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));
    }

    public Optional<EventDTO> getEvent(Long eventId) {
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
