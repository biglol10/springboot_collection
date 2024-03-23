package com.biglol.getinline.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@Table(
        indexes = {
            @Index(columnList = "phoneNumber"),
            @Index(columnList = "createdAt"),
            @Index(columnList = "modifiedAt")
        })
@EntityListeners(AuditingEntityListener.class)
@Entity
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @Column(nullable = false, unique = true)
    private String nickname;

    @Setter
    @Column(nullable = false)
    private String password;

    @Setter
    @Column(nullable = false)
    private String phoneNumber;

    @Setter private String memo;

    @ToString.Exclude // 양방향 관계 걸어줄 때 중요한 건 순환참조문제를 발생시킬 수 있기에 Lombok의 ToString을 사용할 때 조심해야 함.
    // Admin에서 AdminPlaceMap에 들어가면 또 Admin을 볼 수 있어 순환참조.
    // 그래서 한쪽은 ToString에 포함시키지 않도록 해줘야 함 (@ToString.Exclude)
    @OrderBy("id")
    @OneToMany(mappedBy = "admin") // 어느 녀석이 foreign key를 맺고 주인관계가 되느냐를 설정
    private final Set<AdminPlaceMap> adminPlaceMaps = new LinkedHashSet<>();

    @Column(
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "datetime default CURRENT_TIMESTAMP")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP")
    @LastModifiedDate
    private LocalDateTime modifiedAt;

    protected Admin() {}

    protected Admin(
            String email, String nickname, String password, String phoneNumber, String memo) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.memo = memo;
    }

    public static Admin of(
            String email, String nickname, String password, String phoneNumber, String memo) {
        return new Admin(email, nickname, password, phoneNumber, memo);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return id != null && id.equals(((Admin) obj).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, nickname, phoneNumber, createdAt, modifiedAt);
    }
}
