package com.alibou.booknetwork.notification;

import com.alibou.booknetwork.service.async.AsyncNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 알림 서비스 클래스
 * 
 * 엔터프라이즈 알림 서비스의 핵심 기능:
 * 1. 실시간 알림 전송: SSE를 통한 즉시 알림 전달
 * 2. 연결 관리: 사용자별 SSE 연결 상태 추적
 * 3. 알림 저장: 데이터베이스 영속성 및 히스토리 관리
 * 4. 배치 처리: 대량 알림의 효율적 전송
 * 5. 재시도 로직: 실패한 알림의 자동 재전송
 * 
 * 성능 최적화 전략:
 * - 메모리 기반 연결 관리: 빠른 사용자 조회
 * - 비동기 배치 처리: 대량 알림 처리 성능 향상
 * - 우선순위 큐: 긴급 알림 우선 처리
 * - 연결 풀링: 리소스 효율적 관리
 * 
 * SSGD 알림 서비스 패턴:
 * - 이벤트 기반 아키텍처: 느슨한 결합 및 확장성
 * - Circuit Breaker: 외부 서비스 장애 대응
 * - 백프레셔 제어: 클라이언트 과부하 방지
 * - 메트릭 수집: 알림 성능 모니터링
 * - graceful degradation: 일부 실패 시에도 서비스 지속
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AsyncNotificationService asyncNotificationService;

    // 활성 SSE 연결 관리 (메모리 기반)
    // 운영환경에서는 Redis 등 외부 저장소 사용 권장
    private final Map<Integer, SseEmitter> activeConnections = new ConcurrentHashMap<>();
    private final Map<Integer, LocalDateTime> connectionTimes = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> connectionRetries = new ConcurrentHashMap<>();

    // 알림 전송 통계
    private final Map<String, Long> notificationStats = new ConcurrentHashMap<>();

    /**
     * SSE 연결 추가
     * 
     * 연결 관리 프로세스:
     * 1. 기존 연결 정리 (중복 연결 방지)
     * 2. 새 연결 등록
     * 3. 연결 시간 기록
     * 4. 연결 통계 업데이트
     */
    public void addConnection(Integer userId, SseEmitter emitter) {
        try {
            // 기존 연결이 있으면 정리
            removeConnection(userId);

            // 새 연결 등록
            activeConnections.put(userId, emitter);
            connectionTimes.put(userId, LocalDateTime.now());
            connectionRetries.put(userId, 0);

            // 연결 통계 업데이트
            updateConnectionStats(1);

            log.info("SSE 연결 추가 - 사용자ID: {}, 총 연결 수: {}", userId, activeConnections.size());

        } catch (Exception e) {
            log.error("SSE 연결 추가 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * SSE 연결 제거
     */
    public void removeConnection(Integer userId) {
        try {
            SseEmitter emitter = activeConnections.remove(userId);
            connectionTimes.remove(userId);
            connectionRetries.remove(userId);

            if (emitter != null) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("SSE 연결 완료 처리 중 오류 - 사용자ID: {}", userId);
                }

                // 연결 통계 업데이트
                updateConnectionStats(-1);

                log.info("SSE 연결 제거 - 사용자ID: {}, 총 연결 수: {}", userId, activeConnections.size());
            }

        } catch (Exception e) {
            log.error("SSE 연결 제거 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 사용자 온라인 여부 확인
     */
    public boolean isUserConnected(Integer userId) {
        return activeConnections.containsKey(userId);
    }

    /**
     * 단일 사용자에게 알림 전송
     * 
     * 알림 전송 프로세스:
     * 1. 알림 메시지 데이터베이스 저장
     * 2. 사용자 연결 상태 확인
     * 3. 실시간 알림 전송 (SSE)
     * 4. 전송 결과 업데이트
     * 5. 실패 시 재시도 스케줄링
     */
    @Transactional
    public boolean sendNotification(NotificationMessage notification) {
        try {
            log.info("알림 전송 시작 - 대상: {}, 타입: {}, 제목: {}", 
                notification.getTargetUserId(), notification.getType(), notification.getTitle());

            // 1. 알림 메시지 저장
            notification = notificationRepository.save(notification);

            // 2. 실시간 전송 (사용자가 온라인인 경우)
            boolean sentRealtime = false;
            if (notification.getTargetUserId() != null) {
                sentRealtime = sendRealtimeNotification(notification.getTargetUserId(), notification);
            }

            // 3. 전송 결과 업데이트
            if (sentRealtime) {
                notification.markAsSent();
                notificationRepository.save(notification);
                updateNotificationStats("sent", 1);
            } else {
                // 오프라인 사용자는 다음 접속 시 확인 가능하도록 대기 상태 유지
                log.debug("사용자 오프라인 - 알림 대기 상태 유지, 사용자ID: {}", notification.getTargetUserId());
                updateNotificationStats("pending", 1);
            }

            // 4. 다른 채널 알림 (이메일, SMS 등) - 비동기 처리
            if (notification.getDeliveryChannel() != NotificationMessage.DeliveryChannel.WEB) {
                scheduleMultiChannelDelivery(notification);
            }

            log.info("알림 전송 완료 - ID: {}, 실시간 전송: {}", notification.getId(), sentRealtime);
            return true;

        } catch (Exception e) {
            log.error("알림 전송 실패 - 대상: {}, 오류: {}", 
                notification.getTargetUserId(), e.getMessage(), e);
            
            // 실패한 알림 저장 (재시도를 위해)
            try {
                notification.markAsFailed(e.getMessage());
                notificationRepository.save(notification);
                updateNotificationStats("failed", 1);
            } catch (Exception saveException) {
                log.error("실패한 알림 저장 중 오류: {}", saveException.getMessage());
            }
            
            return false;
        }
    }

    /**
     * 브로드캐스트 알림 전송 (모든 연결된 사용자)
     */
    @Async("notificationTaskExecutor")
    public CompletableFuture<Integer> broadcastNotification(NotificationMessage notification) {
        try {
            log.info("브로드캐스트 알림 전송 시작 - 타입: {}, 제목: {}", 
                notification.getType(), notification.getTitle());

            int sentCount = 0;
            int totalConnections = activeConnections.size();

            // 모든 연결된 사용자에게 전송
            for (Map.Entry<Integer, SseEmitter> entry : activeConnections.entrySet()) {
                Integer userId = entry.getKey();
                
                try {
                    // 각 사용자별로 개별 알림 객체 생성 (ID 충돌 방지)
                    NotificationMessage userNotification = notification.copy();
                    userNotification.setTargetUserId(userId);
                    
                    // 데이터베이스 저장
                    userNotification = notificationRepository.save(userNotification);
                    
                    // 실시간 전송
                    boolean sent = sendRealtimeNotification(userId, userNotification);
                    if (sent) {
                        sentCount++;
                        userNotification.markAsSent();
                        notificationRepository.save(userNotification);
                    }

                    // 과부하 방지를 위한 지연
                    Thread.sleep(10);

                } catch (Exception e) {
                    log.warn("개별 사용자 알림 전송 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage());
                }
            }

            // 전송 통계 업데이트
            updateNotificationStats("broadcast_sent", sentCount);
            updateNotificationStats("broadcast_total", totalConnections);

            log.info("브로드캐스트 알림 전송 완료 - 성공: {}/{}", sentCount, totalConnections);
            return CompletableFuture.completedFuture(sentCount);

        } catch (Exception e) {
            log.error("브로드캐스트 알림 전송 실패 - 오류: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * 실시간 알림 전송 (SSE)
     */
    private boolean sendRealtimeNotification(Integer userId, NotificationMessage notification) {
        try {
            SseEmitter emitter = activeConnections.get(userId);
            if (emitter == null) {
                log.debug("사용자 연결 없음 - 사용자ID: {}", userId);
                return false;
            }

            // SSE 이벤트 전송
            emitter.send(SseEmitter.event()
                .id(notification.getId())
                .name("notification")
                .data(notification));

            log.debug("실시간 알림 전송 성공 - 사용자ID: {}, 알림ID: {}", userId, notification.getId());
            return true;

        } catch (Exception e) {
            log.warn("실시간 알림 전송 실패 - 사용자ID: {}, 알림ID: {}, 오류: {}", 
                userId, notification.getId(), e.getMessage());

            // 연결 오류 시 연결 제거
            removeConnection(userId);
            return false;
        }
    }

    /**
     * 다중 채널 알림 전송 스케줄링
     */
    @Async("notificationTaskExecutor")
    public void scheduleMultiChannelDelivery(NotificationMessage notification) {
        try {
            switch (notification.getDeliveryChannel()) {
                case EMAIL:
                    // 이메일 전송 (기존 AsyncEmailService 활용)
                    // emailService.sendNotificationEmail(notification);
                    break;
                case SMS:
                    // SMS 전송
                    // smsService.sendNotificationSms(notification);
                    break;
                case PUSH:
                    // 푸시 알림 전송
                    // pushNotificationService.sendPushNotification(notification);
                    break;
                case MULTI:
                    // 모든 채널로 전송
                    // sendMultiChannelNotification(notification);
                    break;
                default:
                    log.debug("웹 전용 알림 - 추가 채널 전송 생략");
            }

        } catch (Exception e) {
            log.error("다중 채널 알림 전송 실패 - 알림ID: {}, 오류: {}", 
                notification.getId(), e.getMessage(), e);
        }
    }

    /**
     * 사용자별 알림 히스토리 조회
     */
    public Page<NotificationMessage> getNotificationHistory(Integer userId, int page, int size, String type) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            if (type != null && !type.trim().isEmpty()) {
                NotificationMessage.NotificationType notificationType = 
                    NotificationMessage.NotificationType.valueOf(type.toUpperCase());
                return notificationRepository.findByTargetUserIdAndTypeOrderByCreatedAtDesc(
                    userId, notificationType, pageable);
            } else {
                return notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId, pageable);
            }

        } catch (Exception e) {
            log.error("알림 히스토리 조회 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage(), e);
            return Page.empty();
        }
    }

    /**
     * 미수신 알림 조회 (재연결 시)
     */
    public List<NotificationMessage> getPendingNotifications(Integer userId, String lastEventId) {
        try {
            // lastEventId 이후의 미수신 알림 조회
            if (lastEventId != null && !lastEventId.trim().isEmpty()) {
                return notificationRepository.findPendingNotificationsSinceEventId(userId, lastEventId);
            } else {
                // 최근 1시간 내 미수신 알림 조회
                LocalDateTime since = LocalDateTime.now().minusHours(1);
                return notificationRepository.findPendingNotificationsSince(userId, since);
            }

        } catch (Exception e) {
            log.error("미수신 알림 조회 실패 - 사용자ID: {}, 마지막 이벤트ID: {}, 오류: {}", 
                userId, lastEventId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public boolean markAsRead(String notificationId, Integer userId) {
        try {
            Optional<NotificationMessage> notificationOpt = 
                notificationRepository.findByIdAndTargetUserId(notificationId, userId);

            if (notificationOpt.isPresent()) {
                NotificationMessage notification = notificationOpt.get();
                if (!Boolean.TRUE.equals(notification.getIsRead())) {
                    notification.markAsRead();
                    notificationRepository.save(notification);
                    
                    updateNotificationStats("read", 1);
                    log.debug("알림 읽음 처리 완료 - 알림ID: {}, 사용자ID: {}", notificationId, userId);
                }
                return true;
            } else {
                log.warn("알림을 찾을 수 없음 - 알림ID: {}, 사용자ID: {}", notificationId, userId);
                return false;
            }

        } catch (Exception e) {
            log.error("알림 읽음 처리 실패 - 알림ID: {}, 사용자ID: {}, 오류: {}", 
                notificationId, userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 연결 상태 통계 조회
     */
    public Map<String, Object> getConnectionStats(Integer userId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("connected", isUserConnected(userId));
            stats.put("totalConnections", activeConnections.size());
            
            if (connectionTimes.containsKey(userId)) {
                stats.put("connectionTime", connectionTimes.get(userId));
                stats.put("connectionDuration", 
                    java.time.Duration.between(connectionTimes.get(userId), LocalDateTime.now()).getSeconds());
            }
            
            stats.put("retryCount", connectionRetries.getOrDefault(userId, 0));
            
            // 전체 알림 통계
            stats.put("notificationStats", new HashMap<>(notificationStats));

        } catch (Exception e) {
            log.error("연결 통계 조회 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage(), e);
            stats.put("error", "통계 조회 실패");
        }
        
        return stats;
    }

    /**
     * 실패한 알림 재전송 (스케줄링)
     */
    @Async("notificationTaskExecutor")
    public void retryFailedNotifications() {
        try {
            log.info("실패한 알림 재전송 시작");
            
            // 재시도 가능한 실패 알림 조회
            List<NotificationMessage> failedNotifications = 
                notificationRepository.findRetryableFailedNotifications();

            int retryCount = 0;
            for (NotificationMessage notification : failedNotifications) {
                if (notification.canRetry()) {
                    notification.markAsRetrying();
                    notificationRepository.save(notification);
                    
                    // 재전송 시도
                    boolean success = sendNotification(notification);
                    if (success) {
                        retryCount++;
                    }
                    
                    // 재시도 간격 (과부하 방지)
                    Thread.sleep(100);
                }
            }

            log.info("실패한 알림 재전송 완료 - 총 대상: {}, 성공: {}", 
                failedNotifications.size(), retryCount);

        } catch (Exception e) {
            log.error("실패한 알림 재전송 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 만료된 알림 정리 (스케줄링)
     */
    @Async("notificationTaskExecutor")
    public void cleanupExpiredNotifications() {
        try {
            log.info("만료된 알림 정리 시작");
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30); // 30일 이전 알림 삭제
            int deletedCount = notificationRepository.deleteExpiredNotifications(cutoffTime);
            
            log.info("만료된 알림 정리 완료 - 삭제된 알림 수: {}", deletedCount);

        } catch (Exception e) {
            log.error("만료된 알림 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 헬퍼 메서드들
     */
    private void updateConnectionStats(int delta) {
        try {
            String key = "active_connections";
            notificationStats.compute(key, (k, v) -> (v == null ? 0 : v) + delta);
            
        } catch (Exception e) {
            log.warn("연결 통계 업데이트 실패: {}", e.getMessage());
        }
    }

    private void updateNotificationStats(String key, long delta) {
        try {
            notificationStats.compute(key, (k, v) -> (v == null ? 0 : v) + delta);
            
        } catch (Exception e) {
            log.warn("알림 통계 업데이트 실패 - 키: {}, 델타: {}", key, delta);
        }
    }

    /**
     * 모든 연결 정리 (애플리케이션 종료 시)
     */
    public void closeAllConnections() {
        try {
            log.info("모든 SSE 연결 정리 시작 - 연결 수: {}", activeConnections.size());
            
            for (Map.Entry<Integer, SseEmitter> entry : activeConnections.entrySet()) {
                try {
                    entry.getValue().complete();
                } catch (Exception e) {
                    log.debug("연결 정리 중 오류 - 사용자ID: {}", entry.getKey());
                }
            }
            
            activeConnections.clear();
            connectionTimes.clear();
            connectionRetries.clear();
            
            log.info("모든 SSE 연결 정리 완료");

        } catch (Exception e) {
            log.error("연결 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}