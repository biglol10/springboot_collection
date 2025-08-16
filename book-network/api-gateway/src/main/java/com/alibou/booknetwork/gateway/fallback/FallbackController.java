package com.alibou.booknetwork.gateway.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Circuit Breaker Fallback 컨트롤러
 * 
 * 장애 복구 전략 (Resilience Pattern):
 * 1. Graceful Degradation: 부분적 기능 제공
 * 2. Circuit Breaker: 장애 전파 차단
 * 3. Fallback Response: 대안 응답 제공
 * 4. Fast Fail: 빠른 실패로 사용자 경험 개선
 * 
 * 폴백 응답 설계 원칙:
 * - 일관된 응답 형식: 클라이언트 파싱 용이성
 * - 의미있는 오류 메시지: 사용자 이해 가능한 설명
 * - 대안 제안: 가능한 다른 경로 안내
 * - 상태 정보: 서비스 복구 예상 시간
 * 
 * 비즈니스 임팩트:
 * - 사용자 이탈률 감소: 화이트 스크린 대신 안내 메시지
 * - 브랜드 신뢰도 유지: 전문적인 오류 처리
 * - 운영팀 대응 시간 확보: 사용자에게 상황 안내
 * - 서비스 연속성: 부분적 기능이라도 제공
 * 
 * 폴백 모니터링:
 * - 폴백 호출 빈도: 서비스 안정성 지표
 * - 서비스별 장애 패턴: 인프라 개선 방향
 * - 사용자 반응: 폴백 메시지 효과성
 * - 복구 시간: SLA 모니터링
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    /**
     * 사용자 서비스 폴백
     * 
     * 장애 시나리오:
     * - 데이터베이스 연결 장애
     * - 인증 서버 다운
     * - 네트워크 분할
     * - 높은 응답 지연
     * 
     * 폴백 전략:
     * - 캐시된 사용자 정보 제공
     * - 읽기 전용 모드 안내
     * - 대안 인증 방법 제안
     * 
     * @return 사용자 서비스 폴백 응답
     */
    @GetMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn("🚨 User Service 폴백 호출됨 - Circuit Breaker OPEN 상태");
        
        Map<String, Object> fallbackResponse = Map.of(
            "timestamp", LocalDateTime.now(),
            "service", "user-service",
            "status", "temporarily_unavailable",
            "message", "사용자 서비스가 일시적으로 사용할 수 없습니다.",
            "userMessage", "죄송합니다. 계정 관련 기능이 일시적으로 제한됩니다. 잠시 후 다시 시도해 주세요.",
            "alternatives", Map.of(
                "browsing", "도서 검색 및 조회는 계속 이용 가능합니다.",
                "support", "긴급한 경우 고객센터(1588-1234)로 문의해 주세요.",
                "retry", "약 1-2분 후 다시 시도해 주시기 바랍니다."
            ),
            "availableFeatures", new String[]{
                "도서 검색", "카테고리 조회", "인기 도서 보기"
            },
            "estimatedRecovery", "1-2분 이내",
            "errorCode", "USER_SERVICE_UNAVAILABLE"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fallbackResponse);
    }

    /**
     * 도서 서비스 폴백
     * 
     * 장애 시나리오:
     * - 검색 엔진 장애
     * - 도서 데이터베이스 과부하
     * - 외부 API 연동 실패
     * 
     * 폴백 전략:
     * - 캐시된 인기 도서 목록 제공
     * - 기본 카테고리 정보 제공
     * - 단순 검색 기능 제공
     */
    @GetMapping("/book-service")
    public ResponseEntity<Map<String, Object>> bookServiceFallback() {
        log.warn("🚨 Book Service 폴백 호출됨 - Circuit Breaker OPEN 상태");
        
        Map<String, Object> fallbackResponse = Map.of(
            "timestamp", LocalDateTime.now(),
            "service", "book-service",
            "status", "limited_functionality",
            "message", "도서 서비스가 제한된 기능으로 운영 중입니다.",
            "userMessage", "일부 도서 검색 기능이 제한될 수 있습니다. 기본 도서 정보는 이용 가능합니다.",
            "alternatives", Map.of(
                "cache", "인기 도서 및 최신 도서는 조회 가능합니다.",
                "basic", "기본 카테고리별 도서 목록을 확인하세요.",
                "manual", "도서명을 정확히 입력하시면 검색 가능할 수 있습니다."
            ),
            "limitedFeatures", new String[]{
                "상세 검색 필터", "실시간 재고 정보", "유사 도서 추천"
            },
            "availableFeatures", new String[]{
                "기본 도서 정보", "카테고리별 목록", "인기 도서"
            },
            "estimatedRecovery", "30초-1분 이내",
            "errorCode", "BOOK_SERVICE_LIMITED"
        );
        
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fallbackResponse);
    }

    /**
     * 대여 서비스 폴백
     * 
     * 장애 시나리오:
     * - 거래 데이터베이스 장애
     * - 결제 시스템 연동 실패
     * - 재고 시스템 동기화 오류
     * 
     * 폴백 전략:
     * - 대여 신청을 대기열에 저장
     * - 예약 기능으로 대체
     * - 오프라인 서비스 안내
     */
    @GetMapping("/lending-service")
    public ResponseEntity<Map<String, Object>> lendingServiceFallback() {
        log.warn("🚨 Lending Service 폴백 호출됨 - Circuit Breaker OPEN 상태");
        
        Map<String, Object> fallbackResponse = Map.of(
            "timestamp", LocalDateTime.now(),
            "service", "lending-service",
            "status", "maintenance_mode",
            "message", "대여 서비스가 점검 중입니다.",
            "userMessage", "죄송합니다. 도서 대여 기능이 일시적으로 중단되었습니다.",
            "alternatives", Map.of(
                "reservation", "도서 예약 기능을 이용해 주세요.",
                "queue", "대여 신청을 임시 저장해 드릴 수 있습니다.",
                "offline", "가까운 도서관에 직접 방문하시는 것도 가능합니다.",
                "contact", "긴급한 경우 도서관으로 직접 연락해 주세요."
            ),
            "unavailableFeatures", new String[]{
                "즉시 대여", "반납 처리", "연장 신청", "대여 이력 조회"
            },
            "alternativeActions", new String[]{
                "도서 예약", "위시리스트 추가", "알림 설정"
            },
            "estimatedRecovery", "5-10분 이내",
            "priority", "HIGH", // 중요 서비스이므로 우선 복구
            "errorCode", "LENDING_SERVICE_MAINTENANCE"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fallbackResponse);
    }

    /**
     * 추천 서비스 폴백
     * 
     * 장애 시나리오:
     * - AI 모델 서버 다운
     * - 추천 알고리즘 과부하
     * - 사용자 행동 데이터 수집 장애
     * 
     * 폴백 전략:
     * - 인기 도서 기반 추천
     * - 카테고리 기반 추천
     * - 정적 추천 목록 제공
     */
    @GetMapping("/recommendation-service")
    public ResponseEntity<Map<String, Object>> recommendationServiceFallback() {
        log.warn("🚨 Recommendation Service 폴백 호출됨 - Circuit Breaker OPEN 상태");
        
        Map<String, Object> fallbackResponse = Map.of(
            "timestamp", LocalDateTime.now(),
            "service", "recommendation-service",
            "status", "fallback_mode",
            "message", "개인화 추천 서비스를 일시적으로 조정 중입니다.",
            "userMessage", "맞춤 추천 대신 인기 도서를 보여드립니다.",
            "alternatives", Map.of(
                "popular", "현재 가장 인기 있는 도서들을 확인하세요.",
                "category", "관심 카테고리의 신간 도서를 둘러보세요.",
                "search", "직접 검색으로 원하는 도서를 찾아보세요.",
                "later", "잠시 후 다시 방문하시면 개인화 추천을 받으실 수 있습니다."
            ),
            "fallbackData", Map.of(
                "popularBooks", new String[]{"인기도서 1", "인기도서 2", "인기도서 3"},
                "categories", new String[]{"소설", "에세이", "자기계발", "과학"},
                "newArrivals", new String[]{"신간1", "신간2", "신간3"}
            ),
            "estimatedRecovery", "2-3분 이내",
            "impact", "LOW", // 추천은 부가 기능이므로 영향도 낮음
            "errorCode", "RECOMMENDATION_SERVICE_FALLBACK"
        );
        
        return ResponseEntity.status(HttpStatus.OK) // 대안 제공이므로 200 OK
            .contentType(MediaType.APPLICATION_JSON)
            .body(fallbackResponse);
    }

    /**
     * 알림 서비스 폴백
     * 
     * 장애 시나리오:
     * - 실시간 알림 서버 장애
     * - 이메일/SMS 게이트웨이 오류
     * - WebSocket 연결 문제
     * 
     * 폴백 전략:
     * - 알림을 큐에 저장 후 나중에 발송
     * - 중요 알림만 우선 처리
     * - 대안 채널 안내
     */
    @GetMapping("/notification-service")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        log.warn("🚨 Notification Service 폴백 호출됨 - Circuit Breaker OPEN 상태");
        
        Map<String, Object> fallbackResponse = Map.of(
            "timestamp", LocalDateTime.now(),
            "service", "notification-service",
            "status", "queued_mode",
            "message", "알림 서비스가 대기열 모드로 운영 중입니다.",
            "userMessage", "알림 전송이 지연될 수 있습니다. 중요한 알림은 이메일로 발송됩니다.",
            "alternatives", Map.of(
                "email", "중요 알림은 등록된 이메일로 발송됩니다.",
                "refresh", "페이지를 새로고침하여 최신 정보를 확인하세요.",
                "manual", "마이페이지에서 직접 상태를 확인할 수 있습니다.",
                "queue", "모든 알림은 서비스 복구 후 일괄 발송됩니다."
            ),
            "queuedNotifications", true,
            "priorityNotifications", new String[]{
                "대여 만료 알림", "시스템 점검 공지", "보안 관련 알림"
            },
            "estimatedRecovery", "1-2분 이내",
            "impact", "MEDIUM",
            "errorCode", "NOTIFICATION_SERVICE_QUEUED"
        );
        
        return ResponseEntity.status(HttpStatus.ACCEPTED) // 202 Accepted (큐에 저장됨)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fallbackResponse);
    }

    /**
     * 채팅 서비스 폴백
     */
    @GetMapping("/chat-service")
    public ResponseEntity<Map<String, Object>> chatServiceFallback() {
        log.warn("🚨 Chat Service 폴백 호출됨 - Circuit Breaker OPEN 상태");
        
        Map<String, Object> fallbackResponse = Map.of(
            "timestamp", LocalDateTime.now(),
            "service", "chat-service",
            "status", "offline_mode",
            "message", "실시간 채팅 서비스가 일시 중단되었습니다.",
            "userMessage", "실시간 채팅을 이용할 수 없습니다. 다른 방법으로 소통해 주세요.",
            "alternatives", Map.of(
                "email", "문의사항은 이메일(support@booknetwork.com)로 보내주세요.",
                "phone", "긴급한 경우 고객센터(1588-1234)로 연락하세요.",
                "forum", "커뮤니티 게시판을 이용해 주세요.",
                "later", "잠시 후 채팅 서비스가 복구됩니다."
            ),
            "estimatedRecovery", "3-5분 이내",
            "errorCode", "CHAT_SERVICE_OFFLINE"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fallbackResponse);
    }

    /**
     * 분석 서비스 폴백
     */
    @GetMapping("/analytics-service")
    public ResponseEntity<Map<String, Object>> analyticsServiceFallback() {
        log.warn("🚨 Analytics Service 폴백 호출됨 - Circuit Breaker OPEN 상태");
        
        Map<String, Object> fallbackResponse = Map.of(
            "timestamp", LocalDateTime.now(),
            "service", "analytics-service",
            "status", "cached_data_mode",
            "message", "분석 서비스가 캐시 데이터 모드로 운영 중입니다.",
            "userMessage", "최신 통계 대신 이전 데이터를 보여드립니다.",
            "alternatives", Map.of(
                "cached", "지난 시간까지의 데이터는 확인 가능합니다.",
                "basic", "기본 통계 정보는 이용 가능합니다.",
                "export", "데이터 내보내기는 서비스 복구 후 가능합니다.",
                "schedule", "정기 리포트는 예정대로 발송됩니다."
            ),
            "availableData", new String[]{
                "기본 통계", "이전 시간 데이터", "주간/월간 요약"
            },
            "unavailableData", new String[]{
                "실시간 통계", "상세 분석", "데이터 내보내기"
            },
            "estimatedRecovery", "5-10분 이내",
            "impact", "LOW", // 분석은 실시간성이 중요하지 않음
            "errorCode", "ANALYTICS_SERVICE_CACHED"
        );
        
        return ResponseEntity.status(HttpStatus.OK) // 대안 데이터 제공
            .contentType(MediaType.APPLICATION_JSON)
            .body(fallbackResponse);
    }

    /**
     * 일반적인 서비스 폴백 (동적 라우팅)
     * 
     * @param serviceName 서비스 이름
     * @return 일반적인 폴백 응답
     */
    @GetMapping("/{serviceName}")
    public ResponseEntity<Map<String, Object>> genericServiceFallback(@PathVariable String serviceName) {
        log.warn("🚨 {} 폴백 호출됨 - Circuit Breaker OPEN 상태", serviceName);
        
        Map<String, Object> fallbackResponse = Map.of(
            "timestamp", LocalDateTime.now(),
            "service", serviceName,
            "status", "unavailable",
            "message", String.format("%s 서비스가 일시적으로 사용할 수 없습니다.", serviceName),
            "userMessage", "요청하신 서비스가 일시적으로 중단되었습니다. 잠시 후 다시 시도해 주세요.",
            "alternatives", Map.of(
                "retry", "1-2분 후 다시 시도해 주세요.",
                "support", "지속적인 문제 시 고객센터로 문의해 주세요.",
                "status", "서비스 상태는 상태 페이지에서 확인 가능합니다."
            ),
            "estimatedRecovery", "알 수 없음",
            "errorCode", "SERVICE_UNAVAILABLE"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fallbackResponse);
    }
}