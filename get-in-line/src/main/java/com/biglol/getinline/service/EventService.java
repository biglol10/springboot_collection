package com.biglol.getinline.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.biglol.getinline.constant.ErrorCode;
import com.biglol.getinline.domain.Place;
import com.biglol.getinline.exception.GeneralException;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Service;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventDto;
import com.biglol.getinline.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EventService {

    private final EventRepository eventRepository;
//
//    public List<EventDto> getEvents(
//            Long placeId,
//            String eventName,
//            EventStatus eventStatus,
//            LocalDateTime eventStartDatetime,
//            LocalDateTime eventEndDatetime) {
////        return List.of(
////                EventDTO.of(
////                        1L,
////                        "오후 운동",
////                        EventStatus.OPENED,
////                        LocalDateTime.parse("2021-01-01T00:00:00"),
////                        LocalDateTime.parse("2021-02-01T00:00:00"),
////                        0,
////                        24,
////                        "마스크 꼭 착용하세요",
////                        LocalDateTime.now(),
////                        LocalDateTime.now()),
////                EventDTO.of(
////                        1L,
////                        "오후 운동2",
////                        EventStatus.OPENED,
////                        LocalDateTime.parse("2021-03-01T00:00:00"),
////                        LocalDateTime.parse("2021-04-01T00:00:00"),
////                        0,
////                        24,
////                        "마스크 꼭 착용하세요",
////                        LocalDateTime.now(),
////                        LocalDateTime.now()));
//
//        return eventRepository.findEvents(placeId, eventName, eventStatus, eventStartDatetime, eventEndDatetime);
//    }

    public List<EventDto> getEvents(Predicate predicate) {
        try {
            return StreamSupport.stream(eventRepository.findAll(predicate).spliterator(), false)
                    .map(EventDto::of)
                    .toList();
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.DATA_ACCESS_ERROR, e);
        }
    }

    public List<EventDto> getEvents(
            Long placeId,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime
    ) {
        try {
            return null;
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.DATA_ACCESS_ERROR, e);
        }
    }

    public Optional<EventDto> getEvent(Long eventId) {
        try {
            return eventRepository.findById(eventId).map(EventDto::of);
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.DATA_ACCESS_ERROR, e);
        }
    }

    public boolean createEvent(EventDto eventDTO) {
        try {
            if (eventDTO == null) {
                return false;
            }
//
//            Place place = placeRepository.findById(eventDTO.placeDto().id())
//                    .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND));
//            eventRepository.save(eventDTO.toEntity(place));
            return true;
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.DATA_ACCESS_ERROR, e);
        }
    }

    public boolean modifyEvent(Long eventId, EventDto dto) {
        try {
            if (eventId == null || dto == null) {
                return false;
            }

            eventRepository.findById(eventId)
                    .ifPresent(event -> eventRepository.save(dto.updateEntity(event)));

            return true;
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.DATA_ACCESS_ERROR, e);
        }
    }

    public boolean removeEvent(Long eventId) {
        try {
            if (eventId == null) {
                return false;
            }

            eventRepository.deleteById(eventId);
            return true;
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.DATA_ACCESS_ERROR, e);
        }
    }

//    public Optional<EventDto> getEvent(Long eventId) {
//        return eventRepository.findEvent(eventId);
//    }
//
//    public boolean createEvent(EventDto eventDTO) {
//        return eventRepository.insertEvent(eventDTO);
//    }
//
//    public boolean modifyEvent(Long eventIㄱd, EventDto eventDTO) {
//        return eventRepository.updateEvent(eventId, eventDTO);
//    }
//
//    public boolean removeEvent(Long eventId) {
//        return eventRepository.deleteEvent(eventId);
//    }
}
