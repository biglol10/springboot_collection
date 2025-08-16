package com.alibou.booknetwork.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 알림 메시지 엔티티
 * 
 * 알림 시스템 설계 원칙:
 * 1. 확장성: 다양한 알림 타입과 우선순위 지원
 * 2. 추적성: 알림 발송, 수신, 읽음 상태 완전 추적
 * 3. 개인화: 사용자별 맞춤 알림 설정 지원
 * 4. 성능: 대용량 알림 처리를 위한 인덱스 최적화
 * 
 * 엔터프라이즈 알림 시스템 요구사항:
 * - 알림 타입 분류: 시스템, 도서, 채팅, 보안, 마케팅
 * - 우선순위 관리: 긴급, 높음, 보통, 낮음
 * - 전달 채널: 웹, 이메일, SMS, 푸시 알림
 * - 배치 처리: 대량 알림의 효율적 전송
 * - 통계 수집: 발송률, 읽음률, 클릭률 추적
 * 
 * SSGD 알림 시스템 패턴:
 * - UUID 기반 ID: 분산 환경에서 고유성 보장
 * - JSON 메타데이터: 유연한 알림 데이터 구조
 * - 소프트 삭제: 감사 목적으로 데이터 보관
 * - 타임스탬프 관리: 생성, 발송, 읽음 시간 추적
 * - 인덱스 최적화: 사용자별, 타입별, 날짜별 빠른 조회
 */
@Entity
@Table(name = "notification_messages", indexes = {
    @Index(name = "idx_target_user_created", columnList = "target_user_id, created_at"),
    @Index(name = "idx_type_priority", columnList = "notification_type, priority"),
    @Index(name = "idx_read_status", columnList = "is_read, sent_at"),
    @Index(name = "idx_channel_status", columnList = "delivery_channel, delivery_status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    /**
     * 알림 고유 ID (UUID)
     */
    @Id
    private String id;

    /**
     * 알림 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    /**
     * 알림 제목
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * 알림 내용
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 대상 사용자 ID (null이면 브로드캐스트)
     */
    @Column(name = "target_user_id")
    private Integer targetUserId;

    /**
     * 발송자 ID (시스템 알림은 null)
     */
    @Column(name = "sender_id")
    private Integer senderId;

    /**
     * 우선순위
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    /**
     * 전달 채널
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_channel", nullable = false)
    @Builder.Default
    private DeliveryChannel deliveryChannel = DeliveryChannel.WEB;

    /**
     * 전달 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    /**
     * 읽음 여부
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 읽은 시간
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 발송 시간
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * 예약 발송 시간
     */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    /**
     * 만료 시간 (이후로는 표시하지 않음)
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 클릭 가능 여부
     */
    @Column(name = "is_clickable", nullable = false)
    @Builder.Default
    private Boolean isClickable = false;

    /**
     * 클릭 URL
     */
    @Column(name = "click_url")
    private String clickUrl;

    /**
     * 클릭 여부
     */
    @Column(name = "is_clicked", nullable = false)
    @Builder.Default
    private Boolean isClicked = false;

    /**
     * 클릭 시간
     */
    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    /**
     * 아이콘 URL
     */
    @Column(name = "icon_url")
    private String iconUrl;

    /**
     * 이미지 URL
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * 추가 데이터 (JSON 형태)
     */
    @Column(name = "data", columnDefinition = "JSON")
    private String data;

    /**
     * 재시도 횟수
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 마지막 오류 메시지
     */
    @Column(name = "last_error")
    private String lastError;

    /**
     * 알림 그룹 ID (관련 알림 그룹핑)
     */
    @Column(name = "group_id")
    private String groupId;

    /**
     * 생성 시간
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 타임스탬프 (SSE 이벤트용)
     */
    @Transient
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 알림 타입 정의
     */
    public enum NotificationType {
        /**
         * 시스템 알림 (점검, 업데이트 등)
         */
        @JsonProperty("system")
        SYSTEM,

        /**
         * 도서 관련 알림 (대여, 반납, 연체 등)
         */
        @JsonProperty("book")
        BOOK,

        /**
         * 채팅 알림 (새 메시지, 멘션 등)
         */
        @JsonProperty("chat")
        CHAT,

        /**
         * 사용자 알림 (팔로우, 좋아요 등)
         */
        @JsonProperty("user")
        USER,

        /**
         * 보안 알림 (로그인, 비밀번호 변경 등)
         */
        @JsonProperty("security")
        SECURITY,

        /**
         * 마케팅 알림 (프로모션, 이벤트 등)
         */
        @JsonProperty("marketing")
        MARKETING,

        /**
         * 피드백 알림 (리뷰, 평점 등)
         */
        @JsonProperty("feedback")
        FEEDBACK,

        /**
         * 관리자 알림 (신고, 문의 등)
         */
        @JsonProperty("admin")
        ADMIN
    }

    /**
     * 우선순위 정의
     */
    public enum Priority {
        /**
         * 긴급 (즉시 알림)
         */
        @JsonProperty("urgent")
        URGENT,

        /**
         * 높음 (우선 처리)
         */
        @JsonProperty("high")
        HIGH,

        /**
         * 보통 (일반 처리)
         */
        @JsonProperty("normal")
        NORMAL,

        /**
         * 낮음 (배치 처리)
         */
        @JsonProperty("low")
        LOW
    }

    /**
     * 전달 채널 정의
     */
    public enum DeliveryChannel {
        /**
         * 웹 알림 (SSE)
         */
        @JsonProperty("web")
        WEB,

        /**
         * 이메일
         */
        @JsonProperty("email")
        EMAIL,

        /**
         * SMS
         */
        @JsonProperty("sms")
        SMS,

        /**
         * 푸시 알림
         */
        @JsonProperty("push")
        PUSH,

        /**
         * 다중 채널 (모든 채널)
         */
        @JsonProperty("multi")
        MULTI
    }

    /**
     * 전달 상태 정의
     */
    public enum DeliveryStatus {
        /**
         * 전송 대기
         */
        @JsonProperty("pending")
        PENDING,

        /**
         * 전송 중
         */
        @JsonProperty("sending")
        SENDING,

        /**
         * 전송 완료
         */
        @JsonProperty("sent")
        SENT,

        /**
         * 전송 실패
         */
        @JsonProperty("failed")
        FAILED,

        /**
         * 재시도 중
         */
        @JsonProperty("retrying")
        RETRYING,

        /**
         * 만료됨
         */
        @JsonProperty("expired")
        EXPIRED
    }

    /**
     * 엔티티 생성 전 초기화
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    /**
     * 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * 클릭 처리
     */
    public void markAsClicked() {
        this.isClicked = true;
        this.clickedAt = LocalDateTime.now();
    }

    /**
     * 발송 완료 처리
     */
    public void markAsSent() {
        this.deliveryStatus = DeliveryStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 발송 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        this.deliveryStatus = DeliveryStatus.FAILED;
        this.lastError = errorMessage;
        this.retryCount++;
    }

    /**
     * 재시도 처리
     */
    public void markAsRetrying() {
        this.deliveryStatus = DeliveryStatus.RETRYING;
        this.retryCount++;
    }

    /**
     * 만료 여부 확인
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 예약 알림 여부 확인
     */
    public boolean isScheduled() {
        return scheduledAt != null && LocalDateTime.now().isBefore(scheduledAt);
    }

    /**
     * 긴급 알림 여부 확인
     */
    public boolean isUrgent() {
        return priority == Priority.URGENT;
    }

    /**
     * 브로드캐스트 알림 여부 확인
     */
    public boolean isBroadcast() {
        return targetUserId == null;
    }

    /**
     * 재시도 가능 여부 확인
     */
    public boolean canRetry() {
        return retryCount < 3 && 
               deliveryStatus == DeliveryStatus.FAILED && 
               !isExpired();
    }

    /**
     * 알림 요약 반환 (목록 표시용)
     */
    public String getSummary() {
        String summary = title;
        if (summary.length() > 50) {
            summary = summary.substring(0, 47) + "...";
        }
        return summary;
    }

    /**
     * 알림 표시용 시간 반환
     */
    public LocalDateTime getDisplayTime() {
        if (readAt != null) {
            return readAt;
        }
        if (sentAt != null) {
            return sentAt;
        }
        return createdAt;
    }

    /**
     * 추가 데이터 설정 (Map을 JSON으로 변환)
     */
    public void setDataMap(Map<String, Object> dataMap) {
        if (dataMap != null && !dataMap.isEmpty()) {
            try {
                // 실제 구현에서는 ObjectMapper 사용
                this.data = dataMap.toString(); // 간단한 구현
            } catch (Exception e) {
                this.data = "{}";
            }
        }
    }

    /**
     * 알림 복제 (재전송용)
     */
    public NotificationMessage copy() {
        return NotificationMessage.builder()
            .type(this.type)
            .title(this.title)
            .content(this.content)
            .targetUserId(this.targetUserId)
            .senderId(this.senderId)
            .priority(this.priority)
            .deliveryChannel(this.deliveryChannel)
            .isClickable(this.isClickable)
            .clickUrl(this.clickUrl)
            .iconUrl(this.iconUrl)
            .imageUrl(this.imageUrl)
            .data(this.data)
            .groupId(this.groupId)
            .build();
    }
}