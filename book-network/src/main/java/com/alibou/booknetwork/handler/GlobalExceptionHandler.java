package com.alibou.booknetwork.handler;

import com.alibou.booknetwork.exception.OperationNotPermittedException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.alibou.booknetwork.handler.BusinessErrorCodes.*;
import static org.springframework.http.HttpStatus.*;

/**
 * 전역 예외 처리기
 * 
 * 이 클래스는 애플리케이션 전체에서 발생하는 예외를 중앙에서 처리하여
 * 클라이언트에게 일관된 형식의 오류 응답을 제공합니다.
 * 
 * Spring의 @RestControllerAdvice를 사용하여 컨트롤러 계층에서 발생하는
 * 모든 예외를 포착하고, 적절한 HTTP 상태 코드와 함께 구조화된 응답을 반환합니다.
 * 
 * 주요 기능:
 * - 인증/인가 관련 예외 처리
 * - 입력 유효성 검증 예외 처리
 * - 비즈니스 로직 예외 처리
 * - 일반 시스템 예외 처리
 */
@Slf4j // Lombok의 로깅 기능 활성화
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 계정 잠금 예외 처리
     * 
     * @param exception 발생한 LockedException 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(LockedException.class)
    @ResponseStatus(UNAUTHORIZED)
    public ResponseEntity<ExceptionResponse> handleLockedException(
            LockedException exception, 
            HttpServletRequest request) {
        
        log.warn("계정 잠금 예외 발생: {}, 요청 URI: {}", exception.getMessage(), request.getRequestURI());
        
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .businessErrorCode(BusinessErrorCodes.ACCOUNT_LOCKED.getCode())
                                .businessErrorDescription(BusinessErrorCodes.ACCOUNT_LOCKED.getDescription())
                                .error(exception.getMessage())
                                .build()
                );
    }

    /**
     * 계정 비활성화 예외 처리
     * 
     * @param exception 발생한 DisabledException 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(UNAUTHORIZED)
    public ResponseEntity<ExceptionResponse> handleDisabledException(
            DisabledException exception, 
            HttpServletRequest request) {
        
        log.warn("계정 비활성화 예외 발생: {}, 요청 URI: {}", exception.getMessage(), request.getRequestURI());
        
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .businessErrorCode(ACCOUNT_DISABLED.getCode())
                                .businessErrorDescription(ACCOUNT_DISABLED.getDescription())
                                .error(exception.getMessage())
                                .build()
                );
    }

    /**
     * 잘못된 인증 정보 예외 처리
     * 
     * @param exception 발생한 BadCredentialsException 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(UNAUTHORIZED)
    public ResponseEntity<ExceptionResponse> handleBadCredentialsException(
            BadCredentialsException exception,
            HttpServletRequest request) {
        
        log.warn("잘못된 인증 정보 예외 발생, 요청 URI: {}", request.getRequestURI());
        
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .businessErrorCode(BAD_CREDENTIALS.getCode())
                                .businessErrorDescription(BAD_CREDENTIALS.getDescription())
                                .error("로그인 아이디 또는 비밀번호가 올바르지 않습니다.")
                                .build()
                );
    }

    /**
     * 접근 거부 예외 처리
     * 
     * @param exception 발생한 AccessDeniedException 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(FORBIDDEN)
    public ResponseEntity<ExceptionResponse> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request) {
        
        log.warn("접근 거부 예외 발생: {}, 요청 URI: {}", exception.getMessage(), request.getRequestURI());
        
        return ResponseEntity
                .status(FORBIDDEN)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .error("이 작업을 수행할 권한이 없습니다.")
                                .build()
                );
    }

    /**
     * 인증 관련 일반 예외 처리
     * 
     * @param exception 발생한 AuthenticationException 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(UNAUTHORIZED)
    public ResponseEntity<ExceptionResponse> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request) {
        
        log.warn("인증 예외 발생: {}, 요청 URI: {}", exception.getMessage(), request.getRequestURI());
        
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .error("인증에 실패했습니다: " + exception.getMessage())
                                .build()
                );
    }

    /**
     * 이메일 발송 관련 예외 처리
     * 
     * @param exception 발생한 MessagingException 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(MessagingException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ResponseEntity<ExceptionResponse> handleMessagingException(
            MessagingException exception,
            HttpServletRequest request) {
        
        log.error("이메일 발송 예외 발생: {}, 요청 URI: {}", exception.getMessage(), request.getRequestURI(), exception);
        
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .error("이메일 발송 중 오류가 발생했습니다.")
                                .build()
                );
    }

    /**
     * 작업 권한 없음 예외 처리
     * 
     * @param exception 발생한 OperationNotPermittedException 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(OperationNotPermittedException.class)
    @ResponseStatus(FORBIDDEN)
    public ResponseEntity<ExceptionResponse> handleOperationNotPermittedException(
            OperationNotPermittedException exception,
            HttpServletRequest request) {
        
        log.warn("작업 권한 없음 예외 발생: {}, 작업: {}, 리소스: {}, 요청 URI: {}", 
                exception.getMessage(), 
                exception.getOperation(), 
                exception.getResource(), 
                request.getRequestURI());
        
        return ResponseEntity
                .status(FORBIDDEN)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .error(exception.getMessage())
                                .build()
                );
    }

    /**
     * 요청 파라미터 누락 예외 처리
     * 
     * @param exception 발생한 MissingServletRequestParameterException 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<ExceptionResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        
        log.warn("요청 파라미터 누락 예외 발생: {}, 요청 URI: {}", exception.getMessage(), request.getRequestURI());
        
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .error("필수 요청 파라미터가 누락되었습니다: " + exception.getParameterName())
                                .build()
                );
    }

    /**
     * 파라미터 타입 불일치 예외 처리 (@Valid)
     * 
     * @param exception 발생한 MethodArgumentTypeMismatchException 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<ExceptionResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        
        log.warn("파라미터 타입 불일치 예외 발생: {}, 요청 URI: {}", exception.getMessage(), request.getRequestURI());
        
        String paramName = exception.getName();
        String requiredType = exception.getRequiredType() != null ? 
                exception.getRequiredType().getSimpleName() : "unknown";
        
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .error(String.format("파라미터 '%s'의 값이 '%s' 타입으로 변환될 수 없습니다.", 
                                        paramName, requiredType))
                                .build()
                );
    }

    /**
     * 요청 본문 유효성 검증 예외 처리
     * 
     * @param exception 발생한 MethodArgumentNotValidException 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답 (필드별 유효성 검증 오류 포함)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<ExceptionResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        
        log.warn("요청 본문 유효성 검증 예외 발생, 요청 URI: {}", request.getRequestURI());
        
        // 유효성 검증 오류 메시지 수집
        Set<String> validationErrors = new HashSet<>();
        Map<String, String> fieldErrors = new HashMap<>();
        
        exception.getBindingResult().getAllErrors().forEach(error -> {
            String errorMessage = error.getDefaultMessage();
            validationErrors.add(errorMessage);
            
            // 필드별 오류 메시지 (필드명: 오류 메시지)
            if (error instanceof org.springframework.validation.FieldError fieldError) {
                fieldErrors.put(fieldError.getField(), errorMessage);
            }
        });
        
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .error("입력값 검증에 실패했습니다.")
                                .validationErrors(validationErrors)
                                .errors(fieldErrors)
                                .build()
                );
    }

    /**
     * 모든 예외를 처리하는 마지막 단계 핸들러
     * 처리되지 않은 예외를 포착하여 일관된 응답 형식으로 변환합니다.
     * 
     * @param exception 발생한 Exception 객체
     * @param request 현재 HTTP 요청 객체
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ResponseEntity<ExceptionResponse> handleException(
            Exception exception,
            HttpServletRequest request) {
        
        // 심각한 예외는 스택 트레이스를 포함하여 로깅
        log.error("처리되지 않은 예외 발생: {}, 요청 URI: {}", exception.getMessage(), request.getRequestURI(), exception);
        
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(
                        ExceptionResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .path(request.getRequestURI())
                                .businessErrorDescription("내부 서버 오류가 발생했습니다. 관리자에게 문의하세요.")
                                .error(exception.getMessage())
                                .build()
                );
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 고급 로깅 및 모니터링:
 *    - 로깅 확장을 통한 예외 추세 분석
 *    - APM(Application Performance Monitoring) 도구 통합
 *    - 특정 임계치 초과 시 운영팀 자동 알림
 *    
 *    예시:
 *    @Autowired
 *    private MetricsService metricsService;
 *    
 *    private void recordExceptionMetrics(Exception ex, String endpoint) {
 *        metricsService.incrementCounter("api.exceptions", 
 *            "exception_type", ex.getClass().getSimpleName(),
 *            "endpoint", endpoint);
 *    }
 * 
 * 2. 보안 취약점 대응:
 *    - 민감 정보 노출 방지
 *    - 오류 응답 내용 조정으로 정보 유출 차단
 *    - 환경(개발/프로덕션)에 따른 응답 상세 수준 조정
 *    
 *    예시:
 *    @Value("${application.environment:production}")
 *    private String environment;
 *    
 *    private String sanitizeErrorMessage(String message) {
 *        // 프로덕션 환경에서는 상세 오류 숨김
 *        if ("production".equals(environment)) {
 *            return "An internal error occurred";
 *        }
 *        return message;
 *    }
 * 
 * 3. RFC 7807 문제 세부 정보(Problem Details) 구현:
 *    - 표준화된 HTTP API 오류 보고 형식 적용
 *    - 클라이언트 친화적인 오류 응답 제공
 *    - API 문서와 통합된 오류 처리 체계
 *    
 *    예시:
 *    @Data
 *    public class ProblemDetail {
 *        private String type;
 *        private String title;
 *        private int status;
 *        private String detail;
 *        private String instance;
 *        private Map<String, Object> extensions = new HashMap<>();
 *    }
 * 
 * 4. 비즈니스 예외 처리 고도화:
 *    - 도메인별 예외 처리기 분리 및 전용 어노테이션 활용
 *    - 의미 있는 오류 코드 체계 구축 및 문서화
 *    - 다국어 지원을 위한 메시지 국제화
 *    
 *    예시:
 *    @ExceptionHandler
 *    @ResponseStatus(BAD_REQUEST)
 *    public ResponseEntity<ProblemDetail> handleDomainException(
 *            BusinessException ex, HttpServletRequest request, Locale locale) {
 *        
 *        String localizedMessage = messageSource.getMessage(
 *                "error." + ex.getErrorCode(), 
 *                ex.getArgs(), 
 *                ex.getMessage(), 
 *                locale);
 *        
 *        // RFC 7807 형식의 응답 구성
 *    }
 * 
 * 5. 분산 트레이싱 통합:
 *    - 마이크로서비스 환경에서 요청 추적
 *    - OpenTelemetry/Zipkin과 같은 도구 통합
 *    - 특정 사용자/세션의 오류 패턴 분석
 *    
 *    예시:
 *    @Autowired
 *    private Tracer tracer;
 *    
 *    private void enhanceTraceWithExceptionData(Exception ex) {
 *        Span currentSpan = tracer.currentSpan();
 *        if (currentSpan != null) {
 *            currentSpan.tag("error", "true");
 *            currentSpan.tag("error.message", ex.getMessage());
 *            currentSpan.tag("error.type", ex.getClass().getName());
 *        }
 *    }
 * 
 * 6. 점진적 성능 저하(Graceful Degradation) 구현:
 *    - 부분 시스템 장애 시 응급 처리 로직
 *    - 서킷 브레이커 패턴과 통합
 *    - 예외 발생 시 대체 작업 흐름 제공
 * 
 * 7. 인증/인가 예외 처리 고도화:
 *    - 세분화된 권한 오류 피드백
 *    - 임시 액세스 메커니즘 제안
 *    - 잠금 정책 및 복구 프로세스 자동화
 * 
 * 8. 구조화된 이벤트 기반 예외 처리:
 *    - 예외를 이벤트로 발행하여 비동기 처리
 *    - 중앙 집중식 로깅 및 알림 시스템 통합
 *    - 사후 분석 및 개선 프로세스 자동화
 */
