package com.alibou.booknetwork.user;

import com.alibou.booknetwork.book.Book;
import com.alibou.booknetwork.history.BookTransactionHistory;
import com.alibou.booknetwork.role.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 엔티티
 * 
 * 이 클래스는 애플리케이션의 사용자 정보를 표현하는 JPA 엔티티입니다.
 * Spring Security와 통합을 위해 UserDetails 및 Principal 인터페이스를 구현합니다.
 * 이를 통해 인증 및 권한 부여에 필요한 사용자 정보를 제공합니다.
 * 
 * 주요 특징:
 * - 사용자 기본 정보(이름, 이메일 등) 저장
 * - 계정 상태 관리(활성화, 잠금 상태 등)
 * - 사용자 권한 관리
 * - 도서 및 거래 내역과의 관계 정의
 * - 자동 감사 정보 기록(생성일, 수정일)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_user") // SQL에서 'user'는 예약어이므로 다른 이름 사용
@EntityListeners(AuditingEntityListener.class) // 자동 감사 기능 활성화
public class User implements UserDetails, Principal {
    @Id
    @GeneratedValue
    private Integer id;

    private String firstname;
    private String lastname;
    private LocalDate dateOfBirth;
    
    @Column(unique = true) // 이메일 중복 방지
    private String email;
    
    private String password;
    private boolean accountLocked;
    private boolean enabled;

    /**
     * 사용자의 역할 목록
     * 
     * EAGER 패치 전략을 사용하여 사용자 로드 시 함께 로드됩니다.
     * 이는 권한 검사가 빈번하게 일어나기 때문에 성능을 위해 설정되었습니다.
     */
    @ManyToMany(fetch = FetchType.EAGER) // 사용자 로드 시 함께 로드됨
    private List<Role> roles;

    /**
     * 사용자가 소유한 도서 목록
     * 
     * mappedBy 속성은 Book 클래스의 owner 필드를 참조합니다.
     * 양방향 관계에서 주인이 아닌 쪽에 mappedBy를 지정해야 합니다.
     */
    @OneToMany(mappedBy = "owner")
    private List<Book> books;

    /**
     * 사용자의 도서 거래 내역
     */
    @OneToMany(mappedBy = "user")
    private List<BookTransactionHistory> histories;

    /**
     * 엔티티 생성 시간
     * 
     * JPA Auditing 기능을 통해 자동으로 설정됩니다.
     * 생성 후 변경 불가(updatable=false)
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * 엔티티 마지막 수정 시간
     * 
     * 최초 생성 시에는 설정되지 않음(insertable=false)
     */
    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;

    /**
     * Principal 인터페이스의 getName 메소드 구현
     * 
     * 사용자의 고유 식별자로 이메일을 사용합니다.
     */
    @Override
    public String getName() {
        return email;
    }

    /**
     * UserDetails 인터페이스의 getAuthorities 메소드 구현
     * 
     * 사용자의 권한 목록을 반환합니다.
     * Role 엔티티의 이름을 SimpleGrantedAuthority 객체로 변환합니다.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles
                .stream()
                .map(r -> new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * 사용자의 식별자(이메일)를 반환합니다.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * 계정 만료 여부 확인
     * 
     * 현재 구현에서는 계정이 만료되지 않도록 항상 true를 반환합니다.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 계정 잠금 여부 확인
     * 
     * accountLocked 필드의 반대값을 반환합니다.
     */
    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    /**
     * 자격 증명(비밀번호) 만료 여부 확인
     * 
     * 현재 구현에서는 자격 증명이 만료되지 않도록 항상 true를 반환합니다.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 계정 활성화 여부 확인
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 사용자의 전체 이름을 반환합니다.
     * 
     * @return 이름과 성을 합친 전체 이름
     */
    public String fullName() {
        return firstname + " " + lastname;
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 보안 강화:
 *    - 민감 정보 암호화 저장
 *    - 비밀번호 이력 관리 및 재사용 방지
 *    - 로그인 이력 및 실패 횟수 추적
 *    
 *    예시:
 *    @Column(nullable = false)
 *    private Integer failedLoginAttempts = 0;
 *    
 *    @Column
 *    private LocalDateTime lockoutDate;
 *    
 *    @Convert(converter = AttributeEncryptor.class)
 *    private String securityQuestion;
 * 
 * 2. 사용자 프로필 확장:
 *    - 프로필 이미지, 주소, 연락처 등 추가 정보
 *    - 다국어 지원을 위한 언어 선호도
 *    - 알림 설정 및 선호도
 *    
 *    예시:
 *    @Embedded
 *    private Address address;
 *    
 *    @ElementCollection
 *    private Set<String> phoneNumbers = new HashSet<>();
 *    
 *    @Enumerated(EnumType.STRING)
 *    private Locale preferredLocale = Locale.KOREAN;
 * 
 * 3. 소프트 삭제 구현:
 *    - 실제 데이터 삭제 대신 논리적 삭제 처리
 *    - 삭제된 계정 복구 메커니즘
 *    
 *    예시:
 *    @Column(nullable = false)
 *    private boolean deleted = false;
 *    
 *    @Column
 *    private LocalDateTime deletedAt;
 *    
 *    @PreRemove
 *    private void preRemove() {
 *        this.deleted = true;
 *        this.deletedAt = LocalDateTime.now();
 *    }
 * 
 * 4. 계정 레벨 및 보상 시스템:
 *    - 사용자 활동에 따른 포인트/레벨 시스템
 *    - 배지, 업적 등 게임화(Gamification) 요소
 *    
 *    예시:
 *    @Column(nullable = false)
 *    private Integer points = 0;
 *    
 *    @Column(nullable = false)
 *    private Integer level = 1;
 *    
 *    @ManyToMany
 *    private Set<Badge> badges = new HashSet<>();
 * 
 * 5. 사용자 설정 및 환경설정:
 *    - 테마, 레이아웃, 알림 등 사용자 설정
 *    - JSON 형태로 저장하여 유연성 확보
 *    
 *    예시:
 *    @Column(columnDefinition = "jsonb")
 *    @Convert(converter = JsonbConverter.class)
 *    private UserPreferences preferences = new UserPreferences();
 * 
 * 6. 보다 풍부한 감사 정보:
 *    - 생성자, 수정자 정보 자동 기록
 *    - IP 주소, 장치 정보 등 상세 정보 저장
 *    
 *    예시:
 *    @CreatedBy
 *    @Column(updatable = false)
 *    private String createdBy;
 *    
 *    @LastModifiedBy
 *    private String lastModifiedBy;
 *    
 *    @Column(updatable = false)
 *    private String creationIp;
 * 
 * 7. 계정 만료 및 비활성화 정책:
 *    - 계정 자동 만료 일자 관리
 *    - 접속 이력 기반 비활성 계정 처리
 *    
 *    예시:
 *    @Column
 *    private LocalDate accountExpiryDate;
 *    
 *    @Column
 *    private LocalDateTime lastLoginDate;
 *    
 *    public boolean isAccountInactive() {
 *        return lastLoginDate.plusMonths(6).isBefore(LocalDateTime.now());
 *    }
 * 
 * 8. JPA 성능 최적화:
 *    - 연관 관계 로딩 전략 최적화
 *    - 인덱스 및 캐싱 전략 적용
 *    
 *    예시:
 *    @ManyToMany(fetch = FetchType.LAZY)
 *    @BatchSize(size = 20)
 *    private List<Role> roles;
 *    
 *    @Index(name = "idx_user_email", columnList = "email")
 *    @Column(unique = true)
 *    private String email;
 */
