package com.alibou.booknetwork.security.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 보안 예외 응답 클래스
 * 
 * 모든 보안 관련 예외에 대한 표준화된 응답 형식을 제공합니다.
 * SSGD 스타일의 엔터프라이즈급 에러 응답 구조를 따릅니다.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityErrorResponse {

    /**
     * HTTP 상태 코드
     */
    private int status;

    /**
     * 에러 코드 (비즈니스 로직 에러 구분)
     */
    private String errorCode;

    /**
     * 에러 메시지 (사용자에게 표시할 메시지)
     */
    private String message;

    /**
     * 상세 에러 메시지 (디버깅용)
     */
    private String details;

    /**
     * 에러 발생 시각
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 요청 경로
     */
    private String path;

    /**
     * 추적 ID (로깅 및 디버깅용)
     */
    private String traceId;

    /**
     * 추가 정보 (필요에 따라 포함)
     */
    private Map<String, Object> additionalInfo;

    /**
     * 권장 조치 사항
     */
    private String suggestedAction;

    /**
     * 에러 카테고리
     */
    private String category;

    /**
     * 재시도 가능 여부
     */
    private Boolean retryable;

    /**
     * 기본 생성자 메서드들
     */
    public static SecurityErrorResponse of(int status, String errorCode, String message) {
        return SecurityErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SecurityErrorResponse of(int status, String errorCode, String message, String path) {
        return SecurityErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SecurityErrorResponse of(int status, String errorCode, String message, String path, String details) {
        return SecurityErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 토큰 관련 에러 응답
     */
    public static SecurityErrorResponse tokenError(int status, String errorCode, String message, String path) {
        return SecurityErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .category("TOKEN")
                .suggestedAction("Please refresh your token or re-authenticate")
                .retryable(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 인증 관련 에러 응답
     */
    public static SecurityErrorResponse authenticationError(int status, String errorCode, String message, String path) {
        return SecurityErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .category("AUTHENTICATION")
                .suggestedAction("Please provide valid credentials")
                .retryable(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 인가 관련 에러 응답
     */
    public static SecurityErrorResponse authorizationError(int status, String errorCode, String message, String path) {
        return SecurityErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .category("AUTHORIZATION")
                .suggestedAction("Please contact administrator for access")
                .retryable(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 추가 정보를 포함한 에러 응답
     */
    public SecurityErrorResponse withAdditionalInfo(String key, Object value) {
        if (this.additionalInfo == null) {
            this.additionalInfo = new java.util.HashMap<>();
        }
        this.additionalInfo.put(key, value);
        return this;
    }

    /**
     * 추적 ID 설정
     */
    public SecurityErrorResponse withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * 상세 정보 설정
     */
    public SecurityErrorResponse withDetails(String details) {
        this.details = details;
        return this;
    }
}