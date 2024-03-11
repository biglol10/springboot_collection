package com.biglol.getinline.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.biglol.getinline.domain.Place;
import com.biglol.getinline.dto.EventViewResponse;
import com.biglol.getinline.repository.PlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biglol.getinline.constant.ErrorCode;
import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventDto;
import com.biglol.getinline.exception.GeneralException;
import com.biglol.getinline.repository.EventRepository;
import com.querydsl.core.types.Predicate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final PlaceRepository placeRepository;

    public List<EventDto> getEvents(Predicate predicate) {
        try {
            return StreamSupport.stream(eventRepository.findAll(predicate).spliterator(), false)
                    .map(EventDto::of)
                    .toList();
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.DATA_ACCESS_ERROR, e);
        }
    }

    public Page<EventViewResponse> getEventViewResponse(
            String placeName,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime,
            Pageable pageable
    ) {
        try {
            return eventRepository.findEventViewPageBySearchParams(
                    placeName,
                    eventName,
                    eventStatus,
                    eventStartDatetime,
                    eventEndDatetime,
                    pageable
            );
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

            Place place = placeRepository.findById(eventDTO.placeDto().id())
                    .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND));
            eventRepository.save(eventDTO.toEntity(place));
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
}
