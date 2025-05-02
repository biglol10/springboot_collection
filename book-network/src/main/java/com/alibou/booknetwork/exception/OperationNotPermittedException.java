package com.alibou.booknetwork.exception;

import java.time.LocalDateTime;

/**
 * 권한 없는 작업 예외 클래스
 * 
 * 이 예외는 사용자가 수행할 권한이 없는 작업을 시도할 때 발생합니다.
 * 예를 들어, 다른 사용자의 도서를 수정하거나 관리자 전용 기능에 접근하는 경우 등에 사용됩니다.
 * RuntimeException을 상속하여 비검사 예외(Unchecked Exception)로 구현되었으므로,
 * 메서드 시그니처에 throws 키워드를 사용하지 않아도 됩니다.
 * 
 * 이 예외는 GlobalExceptionHandler에서 처리되어 클라이언트에게 적절한 오류 응답을 반환합니다.
 */
public class OperationNotPermittedException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final LocalDateTime timestamp;
    private final String operation;
    private final String resource;
    
    /**
     * 기본 생성자
     * 
     * @param message 예외 메시지
     */
    public OperationNotPermittedException(String message) {
        super(message);
        this.timestamp = LocalDateTime.now();
        this.operation = "UNKNOWN";
        this.resource = "UNKNOWN";
    }
    
    /**
     * 상세 정보를 포함한 생성자
     * 
     * @param message 예외 메시지
     * @param operation 시도된 작업 유형 (예: "UPDATE", "DELETE")
     * @param resource 접근하려 한 리소스 (예: "Book", "User Profile")
     */
    public OperationNotPermittedException(String message, String operation, String resource) {
        super(message);
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.resource = resource;
    }
    
    /**
     * 원인 예외를 포함한 생성자
     * 
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public OperationNotPermittedException(String message, Throwable cause) {
        super(message, cause);
        this.timestamp = LocalDateTime.now();
        this.operation = "UNKNOWN";
        this.resource = "UNKNOWN";
    }
    
    /**
     * 모든 상세 정보를 포함한 생성자
     * 
     * @param message 예외 메시지
     * @param operation 시도된 작업 유형
     * @param resource 접근하려 한 리소스
     * @param cause 원인 예외
     */
    public OperationNotPermittedException(String message, String operation, String resource, Throwable cause) {
        super(message, cause);
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.resource = resource;
    }
    
    /**
     * 예외 발생 시간 반환
     * 
     * @return 예외가 발생한 시간
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * 시도된 작업 유형 반환
     * 
     * @return 작업 유형
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * 접근하려 한 리소스 반환
     * 
     * @return 리소스 정보
     */
    public String getResource() {
        return resource;
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 구조화된 예외 시스템:
 *    - 비즈니스 도메인별로 세분화된 예외 계층 구조 구현
 *    - 에러 코드 체계 도입으로 일관된 오류 처리
 *    
 *    예시:
 *    public enum ErrorCode {
 *        INSUFFICIENT_PERMISSIONS(4001, "사용자에게 필요한 권한이 없습니다"),
 *        RESOURCE_OWNER_MISMATCH(4002, "리소스 소유자만 이 작업을 수행할 수 있습니다"),
 *        // ...
 *    }
 * 
 * 2. 컨텍스트 정보 확장:
 *    - 사용자 ID, 요청 ID 등 디버깅에 유용한 정보 포함
 *    - MDC(Mapped Diagnostic Context)와 통합하여 로깅 강화
 *    
 *    예시:
 *    @Getter
 *    private final String userId;
 *    private final String requestId;
 * 
 * 3. 보안 이벤트 로깅:
 *    - 이벤트 발행을 통한 보안 감사 로깅 자동화
 *    - 의심스러운 액세스 패턴 감지 지원
 *    
 *    예시:
 *    @Autowired
 *    private ApplicationEventPublisher eventPublisher;
 *    
 *    public void publishSecurityEvent() {
 *        eventPublisher.publishEvent(new SecurityViolationEvent(this));
 *    }
 * 
 * 4. REST API 응답 통합:
 *    - RFC 7807 문제 세부 정보(Problem Details) 형식 지원
 *    - API 버전별 응답 형식 관리
 *    
 *    예시:
 *    @Getter
 *    public class ProblemDetail {
 *        private String type;
 *        private String title;
 *        private int status;
 *        private String detail;
 *        private String instance;
 *        // ...
 *    }
 * 
 * 5. 예외 처리 전략 개선:
 *    - 모니터링 및 알림 시스템과 통합
 *    - 특정 임계치 초과 시 자동 알림 발송
 *    
 *    예시:
 *    @Scheduled(fixedRate = 60000)
 *    public void checkSecurityExceptionRate() {
 *        if (securityViolationCounter.getCount() > threshold) {
 *            alertService.sendAlert("Unusually high rate of security violations detected");
 *        }
 *    }
 */
