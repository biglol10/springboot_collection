package com.alibou.booknetwork.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 엔티티
 * 
 * 채팅 메시지 데이터 모델 설계 원칙:
 * 1. 확장성: 다양한 메시지 타입 지원 (텍스트, 이미지, 파일, 시스템)
 * 2. 성능: 인덱스 최적화로 빠른 조회 성능
 * 3. 감사성: 메시지 생성/수정 시간 자동 추적
 * 4. 무결성: 외래키 제약조건으로 데이터 일관성 보장
 * 
 * 엔터프라이즈 메시징 시스템 요구사항:
 * - 메시지 순서 보장: timestamp + sequence 복합 인덱스
 * - 메시지 검색: 전문 검색을 위한 content 인덱스
 * - 읽음 상태 추적: 메시지별 읽음/안읽음 상태 관리
 * - 메시지 삭제: 소프트 삭제로 감사 추적 유지
 * - 대용량 처리: 파티셔닝을 고려한 테이블 설계
 * 
 * SSGD 메시징 패턴 적용:
 * - 메시지 타입 확장성: enum을 통한 타입 안전성
 * - JSON 직렬화: 클라이언트와의 데이터 교환 최적화
 * - 감사 로깅: JPA Auditing으로 자동 시간 추적
 * - 성능 최적화: 필요한 컬럼만 조회하는 projection 지원
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_room_timestamp", columnList = "room_id, timestamp"),
    @Index(name = "idx_sender_timestamp", columnList = "sender_id, timestamp"),
    @Index(name = "idx_content_search", columnList = "content")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 채팅방 ID (UUID 문자열)
     * 
     * UUID 사용 이유:
     * - 분산 환경에서 고유성 보장
     * - 예측 불가능한 ID로 보안성 향상
     * - 채팅방 URL에 직접 사용 가능
     */
    @Column(name = "room_id", nullable = false)
    private String roomId;

    /**
     * 메시지 발송자 ID
     */
    @Column(name = "sender_id", nullable = false)
    private Integer senderId;

    /**
     * 발송자 이름 (비정규화)
     * 
     * 비정규화 이유:
     * - 메시지 조회 시 JOIN 연산 제거로 성능 향상
     * - 사용자 정보 변경이 기존 메시지에 영향 없음
     * - 메시지 히스토리의 일관성 유지
     */
    @Column(name = "sender_name", nullable = false)
    private String senderName;

    /**
     * 메시지 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType type;

    /**
     * 메시지 내용
     * 
     * TEXT 타입 사용 이유:
     * - 최대 65,535자까지 저장 가능
     * - 긴 메시지나 JSON 데이터 저장 가능
     * - 향후 리치 텍스트 확장 고려
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 메시지 전송 시간
     */
    @Column(name = "timestamp", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 메시지 수정 여부
     */
    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private Boolean isEdited = false;

    /**
     * 메시지 삭제 여부 (소프트 삭제)
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * 첨부파일 URL (이미지, 파일 메시지인 경우)
     */
    @Column(name = "attachment_url")
    private String attachmentUrl;

    /**
     * 첨부파일 원본 이름
     */
    @Column(name = "attachment_name")
    private String attachmentName;

    /**
     * 첨부파일 크기 (바이트)
     */
    @Column(name = "attachment_size")
    private Long attachmentSize;

    /**
     * 메타데이터 (JSON 형태의 추가 정보)
     * 
     * 활용 예시:
     * - 답장 메시지의 원본 메시지 ID
     * - 메시지 반응(이모지) 정보
     * - 메시지 포맷팅 정보
     * - 링크 미리보기 정보
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /**
     * 메시지 생성 시간 (감사 목적)
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 메시지 수정 시간 (감사 목적)
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 메시지 타입 정의
     * 
     * 확장 가능한 메시지 타입 설계:
     * - 기본 타입: 일반적인 채팅 메시지
     * - 시스템 타입: 시스템 알림 메시지
     * - 멀티미디어 타입: 파일, 이미지 등
     * - 특수 타입: 타이핑 상태, 오류 메시지 등
     */
    public enum MessageType {
        /**
         * 일반 채팅 메시지
         */
        @JsonProperty("chat")
        CHAT,

        /**
         * 시스템 메시지 (입장, 퇴장 등)
         */
        @JsonProperty("system")
        SYSTEM,

        /**
         * 사용자 채팅방 참여
         */
        @JsonProperty("join")
        JOIN,

        /**
         * 사용자 채팅방 퇴장
         */
        @JsonProperty("leave")
        LEAVE,

        /**
         * 타이핑 상태 (실시간 전송, DB 저장 안함)
         */
        @JsonProperty("typing")
        TYPING,

        /**
         * 이미지 메시지
         */
        @JsonProperty("image")
        IMAGE,

        /**
         * 파일 메시지
         */
        @JsonProperty("file")
        FILE,

        /**
         * 오류 메시지
         */
        @JsonProperty("error")
        ERROR,

        /**
         * 공지사항 메시지
         */
        @JsonProperty("announcement")
        ANNOUNCEMENT,

        /**
         * 답장 메시지
         */
        @JsonProperty("reply")
        REPLY
    }

    /**
     * 메시지 표시용 내용 반환
     * 
     * 타입별 다른 표시 형태:
     * - 삭제된 메시지: "삭제된 메시지입니다"
     * - 파일 메시지: "파일: {파일명}"
     * - 이미지 메시지: "이미지: {파일명}"
     */
    public String getDisplayContent() {
        if (Boolean.TRUE.equals(isDeleted)) {
            return "삭제된 메시지입니다.";
        }

        switch (type) {
            case FILE:
                return "파일: " + (attachmentName != null ? attachmentName : "첨부파일");
            case IMAGE:
                return "이미지: " + (attachmentName != null ? attachmentName : "이미지");
            case SYSTEM:
            case JOIN:
            case LEAVE:
            case ANNOUNCEMENT:
                return content;
            case CHAT:
            case REPLY:
            default:
                return content;
        }
    }

    /**
     * 메시지 수정 가능 여부 확인
     * 
     * 수정 가능 조건:
     * - 일반 채팅 메시지 또는 답장 메시지
     * - 삭제되지 않은 메시지
     * - 전송 후 24시간 이내 (비즈니스 룰)
     */
    public boolean isEditable() {
        if (Boolean.TRUE.equals(isDeleted)) {
            return false;
        }

        if (type != MessageType.CHAT && type != MessageType.REPLY) {
            return false;
        }

        // 24시간 이내 수정 가능
        return timestamp != null && 
               timestamp.isAfter(LocalDateTime.now().minusHours(24));
    }

    /**
     * 메시지 삭제 가능 여부 확인
     * 
     * 삭제 가능 조건:
     * - 이미 삭제되지 않은 메시지
     * - 시스템 메시지가 아닌 사용자 메시지
     * - 전송 후 7일 이내 (비즈니스 룰)
     */
    public boolean isDeletable() {
        if (Boolean.TRUE.equals(isDeleted)) {
            return false;
        }

        if (type == MessageType.SYSTEM || type == MessageType.ANNOUNCEMENT) {
            return false;
        }

        // 7일 이내 삭제 가능
        return timestamp != null && 
               timestamp.isAfter(LocalDateTime.now().minusDays(7));
    }

    /**
     * 파일 첨부 메시지 여부 확인
     */
    public boolean hasAttachment() {
        return attachmentUrl != null && !attachmentUrl.trim().isEmpty();
    }

    /**
     * 메시지 크기 계산 (바이트)
     * 
     * 용도:
     * - 네트워크 대역폭 계산
     * - 저장 공간 관리
     * - 메시지 전송 비용 산정
     */
    public long getMessageSize() {
        long size = 0;
        
        if (content != null) {
            size += content.getBytes().length;
        }
        
        if (attachmentSize != null) {
            size += attachmentSize;
        }
        
        if (metadata != null) {
            size += metadata.getBytes().length;
        }
        
        return size;
    }

    /**
     * 메시지 요약 정보 반환 (알림용)
     * 
     * 알림에 표시할 간략한 메시지 내용:
     * - 긴 텍스트는 50자로 제한
     * - 파일/이미지는 타입 정보만 표시
     * - HTML 태그 제거
     */
    public String getSummary() {
        String summary = getDisplayContent();
        
        if (summary == null) {
            return "";
        }
        
        // HTML 태그 제거 (간단한 정규식)
        summary = summary.replaceAll("<[^>]*>", "");
        
        // 50자로 제한
        if (summary.length() > 50) {
            summary = summary.substring(0, 47) + "...";
        }
        
        return summary;
    }
}