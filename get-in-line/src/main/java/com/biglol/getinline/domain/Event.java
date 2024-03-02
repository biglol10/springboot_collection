package com.biglol.getinline.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import com.biglol.getinline.constant.EventStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@Table(
        indexes = {
            @Index(columnList = "eventName"),
            @Index(columnList = "eventStartDatetime"),
            @Index(columnList = "eventEndDatetime"),
            @Index(columnList = "createdAt"),
            @Index(columnList = "modifiedAt")
        })
@EntityListeners(AuditingEntityListener.class)
@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySql처럼 auto-increment 담당하는게 IDENTITY. MySql일 때 꼭 확인
    private Long id;

    @Setter
    @ManyToOne(optional = false)
    private Place place;

    @Setter
    @Column(nullable = false)
    private String eventName;

    @Setter
    @Column(nullable = false, columnDefinition = "varchar(20) default 'OPENED'")
    @Enumerated(EnumType.STRING)
    private EventStatus eventStatus;

    @Setter
    @Column(nullable = false, columnDefinition = "datetime")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime eventStartDatetime;

    @Setter
    @Column(nullable = false, columnDefinition = "datetime")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime eventEndDatetime;

    @Setter
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer currentNumberOfPeople;

    @Setter
    @Column(nullable = false)
    private Integer capacity;

    @Setter private String memo; // null = true가 기본값이라 @Column안 넣어도 됨

    @Column(
            nullable = false,
            insertable = false, // insert문을 생성시킬 때 column이 추가/포함되지 않는다. JPA를 통해서 insert할 때 column만 뺌
            updatable = false,
            columnDefinition = "datetime default CURRENT_TIMESTAMP") // 이건 schema에다가 기본값을 재현하고 싶어서 넣은거
    @CreatedDate // application level에서 persist하기 전에 영속성 하기 직전에 자동으로 now를 넣어주는 코드를 java코드로 넣은거임. 위랑 목표는 같지만 다른 영역에서 동작. 뺀다면 위를 뺌. 실무에서는 nullable = false 만 넣기도 함
    private LocalDateTime createdAt;

    // 특정한 db기술에 너무 의존하지 않게끔 하고자 하는 목적으로도 ORM을 사용하고 있는건데 columnDefinition에 적은거 보면 MySql문법임. 여기에선 연습을 위해 적은거지만 가능하면 안 적는게 맞음
    @Column(
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP")
    @LastModifiedDate
    private LocalDateTime modifiedAt;

    protected Event() {}

    protected Event(
            Place place,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime,
            Integer currentNumberOfPeople,
            Integer capacity,
            String memo) {
        this.place = place;
        this.eventName = eventName;
        this.eventStatus = eventStatus;
        this.eventStartDatetime = eventStartDatetime;
        this.eventEndDatetime = eventEndDatetime;
        this.currentNumberOfPeople = currentNumberOfPeople;
        this.capacity = capacity;
        this.memo = memo;
    }

    public static Event of(
            Place place,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDatetime,
            LocalDateTime eventEndDatetime,
            Integer currentNumberOfPeople,
            Integer capacity,
            String memo) {
        return new Event(
                place,
                eventName,
                eventStatus,
                eventStartDatetime,
                eventEndDatetime,
                currentNumberOfPeople,
                capacity,
                memo);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return id != null && id.equals(((Event) obj).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventName, eventStartDatetime, eventEndDatetime, createdAt, modifiedAt);
    }
}
