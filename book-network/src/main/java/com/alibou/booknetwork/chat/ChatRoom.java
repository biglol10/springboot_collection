package com.alibou.booknetwork.chat;

import com.alibou.booknetwork.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 채팅방 엔티티
 * 
 * 채팅방 설계 원칙:
 * 1. 확장성: 1:1 채팅부터 대규모 그룹 채팅까지 지원
 * 2. 보안성: 참여자 권한 관리 및 접근 제어
 * 3. 성능: 효율적인 메시지 로딩 및 참여자 관리
 * 4. 유연성: 다양한 채팅방 타입 지원
 * 
 * 엔터프라이즈 채팅 시스템 요구사항:
 * - 참여자 관리: 초대, 퇴장, 권한 관리
 * - 채팅방 설정: 공개/비공개, 최대 참여자 수 제한
 * - 메타데이터: 채팅방 설명, 태그, 카테고리
 * - 통계 정보: 메시지 수, 참여자 수, 활성도
 * - 보관 정책: 메시지 보관 기간, 자동 삭제
 * 
 * SSGD 채팅 시스템 패턴:
 * - UUID 기반 채팅방 ID: 보안성과 확장성
 * - 타입 기반 분류: 1:1, 그룹, 공개 채팅방
 * - 소프트 삭제: 데이터 보관 및 복구 가능
 * - 감사 로깅: 채팅방 생성/수정 이력 추적
 */
@Entity
@Table(name = "chat_rooms", indexes = {
    @Index(name = "idx_room_type_created", columnList = "room_type, created_at"),
    @Index(name = "idx_creator_created", columnList = "creator_id, created_at"),
    @Index(name = "idx_active_rooms", columnList = "is_active, last_message_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    /**
     * 채팅방 고유 ID (UUID)
     */
    @Id
    private String id;

    /**
     * 채팅방 이름
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 채팅방 설명
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 채팅방 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType;

    /**
     * 채팅방 생성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /**
     * 최대 참여자 수 (null이면 무제한)
     */
    @Column(name = "max_participants")
    private Integer maxParticipants;

    /**
     * 현재 참여자 수 (비정규화)
     */
    @Column(name = "current_participants", nullable = false)
    @Builder.Default
    private Integer currentParticipants = 0;

    /**
     * 공개 채팅방 여부
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * 활성 채팅방 여부
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 메시지 히스토리 보관 여부
     */
    @Column(name = "preserve_history", nullable = false)
    @Builder.Default
    private Boolean preserveHistory = true;

    /**
     * 마지막 메시지 시간
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 총 메시지 수 (비정규화)
     */
    @Column(name = "total_messages", nullable = false)
    @Builder.Default
    private Long totalMessages = 0L;

    /**
     * 채팅방 아바타 이미지 URL
     */
    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * 채팅방 태그 (검색용, 쉼표 구분)
     */
    @Column(name = "tags")
    private String tags;

    /**
     * 메타데이터 (JSON 형태의 추가 설정)
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /**
     * 채팅방 생성 시간
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 채팅방 수정 시간
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 채팅방 참여자 목록
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ChatRoomParticipant> participants;

    /**
     * 채팅방 타입 정의
     */
    public enum RoomType {
        /**
         * 1:1 개인 채팅
         */
        DIRECT,

        /**
         * 그룹 채팅 (비공개)
         */
        GROUP,

        /**
         * 공개 채팅방
         */
        PUBLIC,

        /**
         * 도서 관련 채팅방
         */
        BOOK,

        /**
         * 공지사항 채팅방 (읽기 전용)
         */
        ANNOUNCEMENT,

        /**
         * 시스템 채팅방
         */
        SYSTEM
    }

    /**
     * 엔티티 생성 전 초기화
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    /**
     * 새 참여자 추가 가능 여부 확인
     */
    public boolean canAddParticipant() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }

        if (maxParticipants == null) {
            return true; // 무제한
        }

        return currentParticipants < maxParticipants;
    }

    /**
     * 참여자 수 증가
     */
    public void incrementParticipants() {
        if (currentParticipants == null) {
            currentParticipants = 0;
        }
        currentParticipants++;
    }

    /**
     * 참여자 수 감소
     */
    public void decrementParticipants() {
        if (currentParticipants == null || currentParticipants <= 0) {
            currentParticipants = 0;
        } else {
            currentParticipants--;
        }
    }

    /**
     * 메시지 수 증가
     */
    public void incrementMessageCount() {
        if (totalMessages == null) {
            totalMessages = 0L;
        }
        totalMessages++;
        lastMessageAt = LocalDateTime.now();
    }

    /**
     * 채팅방 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 채팅방 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 1:1 채팅방 여부 확인
     */
    public boolean isDirectMessage() {
        return roomType == RoomType.DIRECT;
    }

    /**
     * 그룹 채팅방 여부 확인
     */
    public boolean isGroupChat() {
        return roomType == RoomType.GROUP || roomType == RoomType.PUBLIC;
    }

    /**
     * 공개 채팅방 여부 확인
     */
    public boolean isPublicRoom() {
        return Boolean.TRUE.equals(isPublic) && roomType == RoomType.PUBLIC;
    }

    /**
     * 참여자 수 제한 여부 확인
     */
    public boolean hasParticipantLimit() {
        return maxParticipants != null && maxParticipants > 0;
    }

    /**
     * 채팅방 표시 이름 반환
     * 
     * 1:1 채팅의 경우 상대방 이름 표시 로직 필요
     */
    public String getDisplayName(Integer currentUserId) {
        if (isDirectMessage() && participants != null && participants.size() == 2) {
            // 1:1 채팅의 경우 상대방 이름 반환
            return participants.stream()
                .filter(p -> !p.getUser().getId().equals(currentUserId))
                .map(p -> p.getUser().getFullName())
                .findFirst()
                .orElse(name);
        }
        
        return name;
    }

    /**
     * 채팅방 아바타 URL 반환
     * 
     * 1:1 채팅의 경우 상대방 아바타 표시 로직 필요
     */
    public String getDisplayAvatarUrl(Integer currentUserId) {
        if (isDirectMessage() && participants != null && participants.size() == 2) {
            // 1:1 채팅의 경우 상대방 아바타 반환
            return participants.stream()
                .filter(p -> !p.getUser().getId().equals(currentUserId))
                .map(p -> p.getUser().getAvatarUrl())
                .findFirst()
                .filter(url -> url != null && !url.trim().isEmpty())
                .orElse(avatarUrl);
        }
        
        return avatarUrl;
    }

    /**
     * 채팅방 활성도 계산 (0.0 ~ 1.0)
     * 
     * 활성도 계산 기준:
     * - 최근 메시지 시간
     * - 참여자 수
     * - 메시지 빈도
     */
    public double getActivityScore() {
        if (!Boolean.TRUE.equals(isActive) || lastMessageAt == null) {
            return 0.0;
        }

        // 최근 24시간 내 메시지가 있으면 1.0
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        if (lastMessageAt.isAfter(dayAgo)) {
            return 1.0;
        }

        // 최근 7일 내 메시지가 있으면 0.5
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        if (lastMessageAt.isAfter(weekAgo)) {
            return 0.5;
        }

        // 최근 30일 내 메시지가 있으면 0.2
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        if (lastMessageAt.isAfter(monthAgo)) {
            return 0.2;
        }

        return 0.1; // 비활성 채팅방
    }

    /**
     * 채팅방 요약 정보 반환
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("%s (%s)", name, roomType.name()));
        
        if (currentParticipants != null) {
            summary.append(String.format(" - %d명", currentParticipants));
        }
        
        if (totalMessages != null && totalMessages > 0) {
            summary.append(String.format(" - %d개 메시지", totalMessages));
        }
        
        return summary.toString();
    }
}