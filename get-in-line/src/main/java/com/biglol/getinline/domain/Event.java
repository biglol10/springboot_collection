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

// @EqualsAndHashCode // 제거. 동일성 비교 동등성 비교를 편하게 구현하게 해줌. Entity에서는 부작용. Entity에선 모든 필드가 다 동일하다는 걸
// 검사해야 동등하다 이렇게 평가를 할 필요가 없음. Persistence context가 관리를 하는, 영속성 관리를 해주는 entity는 제대로 등록이 되었다면 id기준으로
// 등록이 됨. 그러니까 id가 동일하면 같은 객체임. 그래서 같은 객체를 실제로 영속성 컨텍스트에서 반환을 해주고 그럼. 그 기준이 id이기에 equals는 id만 가지고
// 구현하면 됨
// @EqualsAndHashCode(onlyExplicitlyIncluded = true) // 위를 해주는 방법. 이걸 delombok해보면 id가 null이면 무조건
// false를 반환 해줘야 함. null이란건 아직 영속성 컨텍스트에 들어가서 id를 부여받지 못했다는 얘기임. 그럴 땐 동등성 비교를 하면 안되고 false를 리턴해줘야 함.
// delombok해보면 id가 null이면 상대방 id도 검사하고 null이면 (null, null) true를 보내줌. 영속성 컨텍스트에 들어가지 않았는데 데이터가 다른
// 엔터티들을 아무리 넣어도 set같은 자료 구조에는 넣을 수가 없게 됨. 그래서 EqualsAndHashCode를 쓰지 않음@Getter
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
    @GeneratedValue(
            strategy =
                    GenerationType.IDENTITY) // MySql처럼 auto-increment 담당하는게 IDENTITY. MySql일 때 꼭 확인
    private Long id;

    @Setter
    @ManyToOne(optional = false) // 무조건 있어야 함
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
            columnDefinition =
                    "datetime default CURRENT_TIMESTAMP") // 이건 schema에다가 기본값을 재현하고 싶어서 넣은거
    @CreatedDate // application level에서 persist하기 전에 영속성 하기 직전에 자동으로 now를 넣어주는 코드를 java코드로 넣은거임. 위랑
    // 목표는 같지만 다른 영역에서 동작. 뺀다면 위를 뺌. 실무에서는 nullable = false 만 넣기도 함
    private LocalDateTime createdAt;

    // 특정한 db기술에 너무 의존하지 않게끔 하고자 하는 목적으로도 ORM을 사용하고 있는건데 columnDefinition에 적은거 보면 MySql문법임. 여기에선 연습을
    // 위해 적은거지만 가능하면 안 적는게 맞음
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

    // 위 이슈 때문에 equals 직접 구현
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return id != null && id.equals(((Event) obj).getId());
    }

    // hashCode도 id만 가지고 만들건데 그 id는 Entity의 영속성 컨텍스트에 등록을 하기 전까지 없음. 그러다가 영속화를 시킬 때 id를 부여받기 때문에
    // hash코드 생성 코드에 id를 개입시키면 이 값이 변한다는거임. 그런데 hash코드는 변하면 안됨. 상수가 들어가야 되기 때문에 직접 구현
    @Override
    public int hashCode() {
        return Objects.hash(eventName, eventStartDatetime, eventEndDatetime, createdAt, modifiedAt);
    }
}
