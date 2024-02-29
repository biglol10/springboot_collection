package com.biglol.getinline.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.biglol.getinline.constant.EventStatus;
import com.biglol.getinline.dto.EventDTO;

// TODO: 인스턴스 설정 관리를 위해 임시로 default 사용. repository layer 구현이 완성되면 삭제
// default를 해두면 RepositoryConfig에서 eventRepository 만들 때 익명 클래스 만들 때 이 안의 내부 구현을 원래 다 해줘야 됨. 그런데
// default로 미리 다 넣어놨으니 그런 부분없이 깔끔하게 되는 꼼수 같은거임
public interface EventRepository {
    default List<EventDTO> findEvents(
            Long placeId,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime) {
        return List.of();
    }

    default Optional<EventDTO> findEvent(Long eventId) {
        return Optional.empty();
    }

    default boolean insertEvent(EventDTO eventDTO) {
        return false;
    }

    default boolean updateEvent(Long eventId, EventDTO dto) {
        return false;
    }

    default boolean deleteEvent(Long eventId) {
        return false;
    }
}
