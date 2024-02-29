package com.biglol.getinline.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventDTO;
import com.biglol.getinline.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EventService {

    private final EventRepository eventRepository;

    public List<EventDTO> getEvents(
            Long placeId,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime) {
//        return List.of(
//                EventDTO.of(
//                        1L,
//                        "오후 운동",
//                        EventStatus.OPENED,
//                        LocalDateTime.parse("2021-01-01T00:00:00"),
//                        LocalDateTime.parse("2021-02-01T00:00:00"),
//                        0,
//                        24,
//                        "마스크 꼭 착용하세요",
//                        LocalDateTime.now(),
//                        LocalDateTime.now()),
//                EventDTO.of(
//                        1L,
//                        "오후 운동2",
//                        EventStatus.OPENED,
//                        LocalDateTime.parse("2021-03-01T00:00:00"),
//                        LocalDateTime.parse("2021-04-01T00:00:00"),
//                        0,
//                        24,
//                        "마스크 꼭 착용하세요",
//                        LocalDateTime.now(),
//                        LocalDateTime.now()));

        return eventRepository.findEvents(placeId, eventName, eventStatus, eventStartDatetime, eventEndDatetime);
    }

    public Optional<EventDTO> getEvent(Long eventId) {
        return eventRepository.findEvent(eventId);
    }

    public boolean createEvent(EventDTO eventDTO) {
        return eventRepository.insertEvent(eventDTO);
    }

    public boolean modifyEvent(Long eventId, EventDTO eventDTO) {
        return eventRepository.updateEvent(eventId, eventDTO);
    }

    public boolean removeEvent(Long eventId) {
        return eventRepository.deleteEvent(eventId);
    }
}
