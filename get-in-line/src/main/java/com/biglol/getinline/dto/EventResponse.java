package com.biglol.getinline.dto;

import java.time.LocalDateTime;

import com.biglol.getinline.constant.EventStatus;

public record EventResponse(
        Long id,
        PlaceDto place,
        String eventName,
        EventStatus eventStatus,
        LocalDateTime eventStartDatetime,
        LocalDateTime eventEndDatetime,
        Integer currentNumberOfPeople,
        Integer capacity,
        String memo) {

    public static EventResponse of(
            Long id,
            PlaceDto place,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime,
            Integer currentNumberOfPeople,
            Integer capacity,
            String memo) {
        return new EventResponse(
                id,
                place,
                eventName,
                eventStatus,
                eventStartDatetime,
                eventEndDatetime,
                currentNumberOfPeople,
                capacity,
                memo);
    }

    public static EventResponse from(EventDto eventDto) {
        if (eventDto == null) {
            return null;
        }
        return EventResponse.of(
                eventDto.id(),
                eventDto.placeDto(),
                eventDto.eventName(),
                eventDto.eventStatus(),
                eventDto.eventStartDatetime(),
                eventDto.eventEndDatetime(),
                eventDto.currentNumberOfPeople(),
                eventDto.capacity(),
                eventDto.memo());
    }

    public static EventResponse empty(PlaceDto placeDto) {
        return EventResponse.of(null, placeDto, null, null, null, null, null, null, null);
    }

    public String getPlaceName() {
        return this.place().placeName();
    }
}
