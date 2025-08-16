package com.alibou.booknetwork.chat;

import com.alibou.booknetwork.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 채팅방 참여자 엔티티
 * 
 * 채팅방 참여자 관리의 핵심 기능:
 * 1. 권한 관리: 관리자, 일반 참여자, 읽기 전용 등
 * 2. 참여 이력: 참여/퇴장 시간 추적
 * 3. 메시지 읽음 상태: 마지막 읽은 메시지 추적
 * 4. 알림 설정: 개인별 알림 on/off 설정
 * 
 * 엔터프라이즈 참여자 관리 요구사항:
 * - 세밀한 권한 제어: 메시지 송신, 파일 업로드, 참여자 초대 등
 * - 참여 이력 보관: 감사 목적 및 통계 분석
 * - 개인화 설정: 알림, 테마, 언어 등
 * - 대규모 그룹 지원: 수천 명 참여자 효율적 관리
 * 
 * SSGD 참여자 관리 패턴:
 * - 복합키 설계: 채팅방 + 사용자 유니크 제약
 * - 소프트 삭제: 퇴장 후에도 이력 보관
 * - 비정규화: 빈번한 조회를 위한 성능 최적화
 * - 인덱스 최적화: 채팅방별, 사용자별 빠른 조회
 */
@Entity
@Table(name = "chat_room_participants", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"room_id", "user_id"})
       },
       indexes = {
           @Index(name = "idx_room_participants", columnList = "room_id, is_active"),
           @Index(name = "idx_user_rooms", columnList = "user_id, is_active"),
           @Index(name = "idx_last_read", columnList = "last_read_at")
       })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 채팅방 정보
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 참여자 정보
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 참여자 역할/권한
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private ParticipantRole role = ParticipantRole.MEMBER;

    /**
     * 활성 참여자 여부 (퇴장 시 false)
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 알림 수신 여부
     */
    @Column(name = "notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean notificationsEnabled = true;

    /**
     * 마지막으로 읽은 메시지 시간
     */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    /**
     * 마지막으로 읽은 메시지 ID
     */
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    /**
     * 읽지 않은 메시지 수 (비정규화)
     */
    @Column(name = "unread_count", nullable = false)
    @Builder.Default
    private Integer unreadCount = 0;

    /**
     * 채팅방 참여 시간
     */
    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    /**
     * 채팅방 퇴장 시간
     */
    @Column(name = "left_at")
    private LocalDateTime leftAt;

    /**
     * 마지막 활동 시간 (메시지 읽기, 타이핑 등)
     */
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    /**
     * 참여자 별명 (채팅방 내에서만 사용)
     */
    @Column(name = "nickname")
    private String nickname;

    /**
     * 음소거 설정 종료 시간 (null이면 음소거 안함)
     */
    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;

    /**
     * 참여자 메타데이터 (JSON 형태)
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /**
     * 레코드 생성 시간
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 레코드 수정 시간
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 참여자 역할 정의
     */
    public enum ParticipantRole {
        /**
         * 채팅방 소유자 (모든 권한)
         */
        OWNER,

        /**
         * 관리자 (참여자 관리, 메시지 삭제 등)
         */
        ADMIN,

        /**
         * 조정자 (메시지 삭제, 참여자 음소거 등)
         */
        MODERATOR,

        /**
         * 일반 참여자 (메시지 송수신)
         */
        MEMBER,

        /**
         * 읽기 전용 참여자
         */
        READONLY,

        /**
         * 게스트 (제한된 권한)
         */
        GUEST
    }

    /**
     * 채팅방 퇴장 처리
     */
    public void leaveChatRoom() {
        this.isActive = false;
        this.leftAt = LocalDateTime.now();
    }

    /**
     * 채팅방 재참여 처리
     */
    public void rejoinChatRoom() {
        this.isActive = true;
        this.leftAt = null;
        this.joinedAt = LocalDateTime.now();
    }

    /**
     * 메시지 읽음 상태 업데이트
     */
    public void markAsRead(Long messageId, LocalDateTime readTime) {
        this.lastReadMessageId = messageId;
        this.lastReadAt = readTime;
        this.unreadCount = 0;
        updateLastActivity();
    }

    /**
     * 읽지 않은 메시지 수 증가
     */
    public void incrementUnreadCount() {
        if (unreadCount == null) {
            unreadCount = 0;
        }
        unreadCount++;
    }

    /**
     * 읽지 않은 메시지 수 초기화
     */
    public void resetUnreadCount() {
        this.unreadCount = 0;
    }

    /**
     * 마지막 활동 시간 업데이트
     */
    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * 음소거 설정
     */
    public void mute(LocalDateTime until) {
        this.mutedUntil = until;
    }

    /**
     * 음소거 해제
     */
    public void unmute() {
        this.mutedUntil = null;
    }

    /**
     * 현재 음소거 상태 확인
     */
    public boolean isMuted() {
        return mutedUntil != null && mutedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * 메시지 전송 권한 확인
     */
    public boolean canSendMessage() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }

        if (isMuted()) {
            return false;
        }

        return role != ParticipantRole.READONLY;
    }

    /**
     * 파일 업로드 권한 확인
     */
    public boolean canUploadFile() {
        if (!canSendMessage()) {
            return false;
        }

        // 게스트는 파일 업로드 불가
        return role != ParticipantRole.GUEST;
    }

    /**
     * 다른 참여자 초대 권한 확인
     */
    public boolean canInviteParticipants() {
        return role == ParticipantRole.OWNER || 
               role == ParticipantRole.ADMIN || 
               role == ParticipantRole.MODERATOR;
    }

    /**
     * 메시지 삭제 권한 확인
     */
    public boolean canDeleteMessage(ChatMessage message) {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }

        // 소유자, 관리자, 조정자는 모든 메시지 삭제 가능
        if (role == ParticipantRole.OWNER || 
            role == ParticipantRole.ADMIN || 
            role == ParticipantRole.MODERATOR) {
            return true;
        }

        // 일반 참여자는 본인 메시지만 삭제 가능
        return message != null && user.getId().equals(message.getSenderId());
    }

    /**
     * 참여자 관리 권한 확인 (추방, 역할 변경 등)
     */
    public boolean canManageParticipant(ChatRoomParticipant targetParticipant) {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }

        // 본인은 관리할 수 없음
        if (this.user.getId().equals(targetParticipant.getUser().getId())) {
            return false;
        }

        // 소유자는 모든 참여자 관리 가능
        if (role == ParticipantRole.OWNER) {
            return true;
        }

        // 관리자는 조정자, 일반 참여자, 게스트 관리 가능
        if (role == ParticipantRole.ADMIN) {
            return targetParticipant.getRole() == ParticipantRole.MODERATOR ||
                   targetParticipant.getRole() == ParticipantRole.MEMBER ||
                   targetParticipant.getRole() == ParticipantRole.READONLY ||
                   targetParticipant.getRole() == ParticipantRole.GUEST;
        }

        // 조정자는 일반 참여자, 게스트만 관리 가능
        if (role == ParticipantRole.MODERATOR) {
            return targetParticipant.getRole() == ParticipantRole.MEMBER ||
                   targetParticipant.getRole() == ParticipantRole.READONLY ||
                   targetParticipant.getRole() == ParticipantRole.GUEST;
        }

        return false;
    }

    /**
     * 알림 수신 여부 토글
     */
    public void toggleNotifications() {
        this.notificationsEnabled = !Boolean.TRUE.equals(this.notificationsEnabled);
    }

    /**
     * 참여 기간 계산 (일 단위)
     */
    public long getParticipationDays() {
        LocalDateTime endTime = Boolean.TRUE.equals(isActive) ? LocalDateTime.now() : leftAt;
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        return java.time.Duration.between(joinedAt, endTime).toDays();
    }

    /**
     * 활성 참여자 여부 확인 (최근 7일 내 활동)
     */
    public boolean isActiveParticipant() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }

        if (lastActivityAt == null) {
            return false;
        }

        return lastActivityAt.isAfter(LocalDateTime.now().minusDays(7));
    }

    /**
     * 참여자 표시 이름 반환 (별명 우선)
     */
    public String getDisplayName() {
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname;
        }
        return user != null ? user.getFullName() : "Unknown";
    }

    /**
     * 참여자 요약 정보 반환
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(getDisplayName());
        summary.append(" (").append(role.name()).append(")");
        
        if (!Boolean.TRUE.equals(isActive)) {
            summary.append(" - 퇴장함");
        } else if (isMuted()) {
            summary.append(" - 음소거됨");
        }
        
        if (unreadCount != null && unreadCount > 0) {
            summary.append(" - ").append(unreadCount).append("개 안읽음");
        }
        
        return summary.toString();
    }
}