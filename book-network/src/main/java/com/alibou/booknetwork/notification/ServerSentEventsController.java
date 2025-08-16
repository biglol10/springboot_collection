package com.alibou.booknetwork.notification;

import com.alibou.booknetwork.security.JwtService;
import com.alibou.booknetwork.user.User;
import com.alibou.booknetwork.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Server-Sent Events 컨트롤러
 * 
 * SSE(Server-Sent Events)의 엔터프라이즈 가치:
 * 1. 실시간 알림: 브라우저에 즉시 알림 전달
 * 2. 낮은 리소스 사용: WebSocket 대비 가벼운 실시간 통신
 * 3. 자동 재연결: 브라우저가 연결 끊김 시 자동 재시도
 * 4. 단방향 통신: 서버 → 클라이언트 알림에 최적화
 * 
 * SSE vs WebSocket 비교:
 * - SSE: 단방향, HTTP 기반, 자동 재연결, 브라우저 네이티브 지원
 * - WebSocket: 양방향, TCP 기반, 수동 재연결, 라이브러리 필요
 * - 사용 사례: SSE는 알림, WebSocket은 채팅/게임
 * 
 * SSGD 실시간 알림 패턴:
 * - JWT 기반 인증: 연결 시 토큰 검증
 * - 사용자별 알림 스트림: 개인화된 알림 채널
 * - 알림 타입 분류: 시스템, 도서, 채팅, 보안 알림
 * - 백프레셔 제어: 클라이언트 처리 능력 고려
 * - 연결 관리: 메모리 누수 방지 및 정리
 * 
 * 엔터프라이즈 알림 시스템 요구사항:
 * - 대규모 동시 연결 지원 (10,000+ 사용자)
 * - 알림 우선순위 관리 (긴급, 중요, 일반)
 * - 알림 배치 처리 (네트워크 효율성)
 * - 오프라인 알림 저장 (재접속 시 미수신 알림 전달)
 * - 알림 통계 수집 (전송률, 읽음률, 반응률)
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class ServerSentEventsController {

    private final NotificationService notificationService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * SSE 연결 수립
     * 
     * SSE 연결 프로세스:
     * 1. JWT 토큰 검증 및 사용자 인증
     * 2. SseEmitter 생성 (타임아웃 30분)
     * 3. 연결 이벤트 전송 (클라이언트 확인용)
     * 4. 사용자별 연결 관리자에 등록
     * 5. 미수신 알림 일괄 전송
     * 
     * @param token JWT 인증 토큰
     * @param lastEventId 마지막 수신 이벤트 ID (재연결 시 사용)
     * @param request HTTP 요청 객체
     * @return SseEmitter 객체
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications(
            @RequestParam(name = "token", required = false) String token,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            HttpServletRequest request,
            Authentication authentication) {
        
        try {
            log.info("SSE 연결 요청 - IP: {}, User-Agent: {}", 
                getClientIP(request), request.getHeader("User-Agent"));

            // 인증 처리 (토큰 또는 Authentication 사용)
            User user = authenticateUser(token, authentication);
            if (user == null) {
                log.warn("SSE 연결 인증 실패 - IP: {}", getClientIP(request));
                return ResponseEntity.status(401).build();
            }

            // SseEmitter 생성 (30분 타임아웃)
            SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
            
            // 연결 관리자에 등록
            notificationService.addConnection(user.getId(), emitter);

            // 연결 성공 이벤트 전송
            sendConnectionEvent(emitter, user);

            // 미수신 알림 전송 (재연결 시)
            if (lastEventId != null) {
                sendPendingNotifications(emitter, user.getId(), lastEventId);
            }

            // 연결 종료 시 정리 작업
            setupEmitterCallbacks(emitter, user.getId());

            log.info("SSE 연결 성공 - 사용자: {}, IP: {}", user.getEmail(), getClientIP(request));
            return ResponseEntity.ok(emitter);

        } catch (Exception e) {
            log.error("SSE 연결 실패 - IP: {}, 오류: {}", getClientIP(request), e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 특정 사용자에게 알림 전송 (테스트용)
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @RequestBody SendNotificationRequest request,
            Authentication authentication) {
        
        try {
            User sender = getUserFromAuthentication(authentication);
            if (sender == null) {
                return ResponseEntity.status(401).build();
            }

            // 관리자 권한 확인
            if (!hasAdminRole(sender)) {
                log.warn("알림 전송 권한 없음 - 사용자: {}", sender.getEmail());
                return ResponseEntity.status(403).body(
                    Map.of("error", "알림 전송 권한이 없습니다."));
            }

            // 알림 생성 및 전송
            NotificationMessage notification = NotificationMessage.builder()
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .targetUserId(request.getTargetUserId())
                .priority(request.getPriority())
                .data(request.getData())
                .build();

            boolean sent = notificationService.sendNotification(notification);

            if (sent) {
                log.info("알림 전송 성공 - 대상: {}, 제목: {}", 
                    request.getTargetUserId(), request.getTitle());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "알림이 성공적으로 전송되었습니다."
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "알림 전송에 실패했습니다."
                ));
            }

        } catch (Exception e) {
            log.error("알림 전송 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                Map.of("error", "알림 전송 중 오류가 발생했습니다."));
        }
    }

    /**
     * 브로드캐스트 알림 전송 (모든 사용자)
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastNotification(
            @RequestBody BroadcastNotificationRequest request,
            Authentication authentication) {
        
        try {
            User sender = getUserFromAuthentication(authentication);
            if (sender == null || !hasAdminRole(sender)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "브로드캐스트 권한이 없습니다."));
            }

            // 브로드캐스트 알림 생성
            NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.SYSTEM)
                .title(request.getTitle())
                .content(request.getContent())
                .priority(request.getPriority())
                .data(request.getData())
                .build();

            // 비동기로 모든 연결된 사용자에게 전송
            CompletableFuture<Integer> sendResult = notificationService.broadcastNotification(notification);
            
            sendResult.thenAccept(sentCount -> {
                log.info("브로드캐스트 알림 전송 완료 - 전송 수: {}, 제목: {}", sentCount, request.getTitle());
            });

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "브로드캐스트 알림이 전송되었습니다."
            ));

        } catch (Exception e) {
            log.error("브로드캐스트 알림 전송 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                Map.of("error", "브로드캐스트 알림 전송 중 오류가 발생했습니다."));
        }
    }

    /**
     * 사용자별 알림 히스토리 조회
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getNotificationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            Authentication authentication) {
        
        try {
            User user = getUserFromAuthentication(authentication);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }

            // 알림 히스토리 조회 (페이징)
            var notifications = notificationService.getNotificationHistory(user.getId(), page, size, type);

            return ResponseEntity.ok(Map.of(
                "notifications", notifications.getContent(),
                "totalElements", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages(),
                "currentPage", page
            ));

        } catch (Exception e) {
            log.error("알림 히스토리 조회 중 오류 발생 - 사용자: {}, 오류: {}", 
                getUserFromAuthentication(authentication).getEmail(), e.getMessage(), e);
            return ResponseEntity.status(500).body(
                Map.of("error", "알림 히스토리 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 알림 읽음 처리
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable String notificationId,
            Authentication authentication) {
        
        try {
            User user = getUserFromAuthentication(authentication);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }

            boolean marked = notificationService.markAsRead(notificationId, user.getId());

            if (marked) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "알림이 읽음 처리되었습니다."
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "알림을 찾을 수 없거나 이미 읽음 처리되었습니다."
                ));
            }

        } catch (Exception e) {
            log.error("알림 읽음 처리 중 오류 발생 - 알림ID: {}, 오류: {}", notificationId, e.getMessage(), e);
            return ResponseEntity.status(500).body(
                Map.of("error", "알림 읽음 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 연결 상태 확인 (헬스 체크)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getConnectionHealth(Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }

            boolean connected = notificationService.isUserConnected(user.getId());
            Map<String, Object> health = notificationService.getConnectionStats(user.getId());

            return ResponseEntity.ok(Map.of(
                "connected", connected,
                "stats", health,
                "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("연결 상태 확인 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 사용자 인증 처리
     */
    private User authenticateUser(String token, Authentication authentication) {
        try {
            // 1. JWT 토큰으로 인증 시도
            if (token != null && !token.trim().isEmpty()) {
                if (jwtService.isTokenValid(token)) {
                    String userEmail = jwtService.extractUsername(token);
                    return userRepository.findByEmail(userEmail).orElse(null);
                }
            }

            // 2. Authentication 객체로 인증 시도
            if (authentication != null && authentication.isAuthenticated()) {
                String userEmail = authentication.getName();
                return userRepository.findByEmail(userEmail).orElse(null);
            }

            return null;

        } catch (Exception e) {
            log.error("사용자 인증 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 연결 성공 이벤트 전송
     */
    private void sendConnectionEvent(SseEmitter emitter, User user) {
        try {
            NotificationMessage connectEvent = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.SYSTEM)
                .title("연결 성공")
                .content("실시간 알림에 연결되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();

            emitter.send(SseEmitter.event()
                .id("connect-" + System.currentTimeMillis())
                .name("connect")
                .data(connectEvent));

            log.debug("연결 이벤트 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.warn("연결 이벤트 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * 미수신 알림 전송 (재연결 시)
     */
    private void sendPendingNotifications(SseEmitter emitter, Integer userId, String lastEventId) {
        try {
            CompletableFuture.runAsync(() -> {
                var pendingNotifications = notificationService.getPendingNotifications(userId, lastEventId);
                
                for (var notification : pendingNotifications) {
                    try {
                        emitter.send(SseEmitter.event()
                            .id(notification.getId())
                            .name("notification")
                            .data(notification));
                            
                        // 전송 간격 조절 (클라이언트 과부하 방지)
                        Thread.sleep(50);
                        
                    } catch (Exception e) {
                        log.warn("미수신 알림 전송 실패 - 사용자ID: {}, 알림ID: {}", 
                            userId, notification.getId());
                        break;
                    }
                }
                
                log.info("미수신 알림 전송 완료 - 사용자ID: {}, 알림 수: {}", 
                    userId, pendingNotifications.size());
            });

        } catch (Exception e) {
            log.error("미수신 알림 전송 중 오류 발생 - 사용자ID: {}, 오류: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * SseEmitter 콜백 설정
     */
    private void setupEmitterCallbacks(SseEmitter emitter, Integer userId) {
        // 연결 완료 시
        emitter.onCompletion(() -> {
            notificationService.removeConnection(userId);
            log.info("SSE 연결 완료 - 사용자ID: {}", userId);
        });

        // 타임아웃 시
        emitter.onTimeout(() -> {
            notificationService.removeConnection(userId);
            log.info("SSE 연결 타임아웃 - 사용자ID: {}", userId);
        });

        // 오류 발생 시
        emitter.onError((throwable) -> {
            notificationService.removeConnection(userId);
            log.warn("SSE 연결 오류 - 사용자ID: {}, 오류: {}", userId, throwable.getMessage());
        });
    }

    /**
     * 헬퍼 메서드들
     */
    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail).orElse(null);
    }

    private boolean hasAdminRole(User user) {
        // 실제 구현에서는 사용자 권한 확인 로직 구현
        return user != null && user.getRole() != null && 
               user.getRole().name().contains("ADMIN");
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.trim().isEmpty()) {
            return xRealIP.trim();
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 요청 DTO 클래스들
     */
    public static class SendNotificationRequest {
        private NotificationMessage.NotificationType type;
        private String title;
        private String content;
        private Integer targetUserId;
        private NotificationMessage.Priority priority;
        private Map<String, Object> data;

        // Getters and Setters
        public NotificationMessage.NotificationType getType() { return type; }
        public void setType(NotificationMessage.NotificationType type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Integer getTargetUserId() { return targetUserId; }
        public void setTargetUserId(Integer targetUserId) { this.targetUserId = targetUserId; }
        public NotificationMessage.Priority getPriority() { return priority; }
        public void setPriority(NotificationMessage.Priority priority) { this.priority = priority; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    public static class BroadcastNotificationRequest {
        private String title;
        private String content;
        private NotificationMessage.Priority priority;
        private Map<String, Object> data;

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public NotificationMessage.Priority getPriority() { return priority; }
        public void setPriority(NotificationMessage.Priority priority) { this.priority = priority; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}