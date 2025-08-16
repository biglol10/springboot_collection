package com.alibou.booknetwork.alert;

import com.alibou.booknetwork.logging.StructuredLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 지능형 알림 관리 서비스
 * 
 * 이 클래스는 시스템 장애, 성능 저하, 보안 위협 등을 실시간으로 감지하고
 * 적절한 담당자에게 자동으로 알림을 발송합니다.
 * 
 * 주요 기능:
 * 1. 다계층 알림 시스템 (INFO -> WARN -> ERROR -> CRITICAL)
 * 2. 알림 중복 제거 및 집계
 * 3. 에스컬레이션 정책 자동 적용
 * 4. 다중 채널 알림 (이메일, Slack, SMS, 웹훅)
 * 5. 스마트 알림 필터링 및 우선순위 지정
 * 
 * 왜 필요한가?
 * - 장애 조기 감지 및 빠른 대응
 * - 운영팀 부담 감소
 * - 서비스 가용성 향상
 * - 고객 영향 최소화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final StructuredLogger structuredLogger;
    private final AlertChannelManager channelManager;
    private final AlertRuleEngine ruleEngine;
    private final AlertHistoryRepository alertHistoryRepository;

    /**
     * 시스템 메트릭 기반 알림 처리
     */
    @Async("alertExecutor")
    public CompletableFuture<Void> processSystemAlert(SystemAlert alert) {
        try {
            log.info("시스템 알림 처리 시작: {}", alert.getAlertName());
            
            // 1. 알림 규칙 엔진으로 필터링 및 우선순위 결정
            AlertDecision decision = ruleEngine.evaluateAlert(alert);
            
            if (decision.shouldSuppress()) {
                log.debug("알림 억제됨: {} (이유: {})", alert.getAlertName(), decision.getSuppressionReason());
                return CompletableFuture.completedFuture(null);
            }

            // 2. 알림 집계 및 중복 제거
            AlertSummary summary = aggregateAlert(alert, decision);
            
            // 3. 심각도에 따른 알림 채널 선택
            List<AlertChannel> channels = selectAlertChannels(summary.getSeverity());
            
            // 4. 각 채널로 알림 발송
            CompletableFuture<Void> sendFuture = sendToChannels(summary, channels);
            
            // 5. 알림 히스토리 저장
            saveAlertHistory(summary);
            
            // 6. 에스컬레이션 스케줄링
            scheduleEscalationIfNeeded(summary);
            
            // 7. 구조화된 로깅
            logAlertEvent(summary);
            
            return sendFuture;
            
        } catch (Exception e) {
            log.error("시스템 알림 처리 중 오류 발생: {}", alert.getAlertName(), e);
            
            // 알림 처리 실패도 중요한 이벤트이므로 별도 알림
            sendCriticalAlert("AlertService 장애", 
                            "알림 처리 시스템에 장애가 발생했습니다: " + e.getMessage());
            
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 비즈니스 로직 관련 알림 처리
     */
    @Async("alertExecutor")
    public CompletableFuture<Void> processBusinessAlert(BusinessAlert alert) {
        try {
            log.info("비즈니스 알림 처리 시작: {}", alert.getEventType());
            
            // 비즈니스 규칙에 따른 알림 처리
            BusinessAlertDecision decision = ruleEngine.evaluateBusinessAlert(alert);
            
            if (decision.requiresImmedateAction()) {
                // 즉시 조치가 필요한 경우 (예: 결제 실패, 보안 위협)
                sendUrgentBusinessAlert(alert, decision);
            } else if (decision.requiresNotification()) {
                // 일반적인 비즈니스 이벤트 알림
                sendBusinessNotification(alert, decision);
            }
            
            // 비즈니스 메트릭 업데이트
            updateBusinessMetrics(alert);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("비즈니스 알림 처리 중 오류 발생: {}", alert.getEventType(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 보안 이벤트 알림 처리
     */
    @Async("alertExecutor")
    public CompletableFuture<Void> processSecurityAlert(SecurityAlert alert) {
        try {
            log.warn("보안 알림 처리 시작: {} (위험도: {})", 
                    alert.getEventType(), alert.getRiskLevel());
            
            // 보안 이벤트는 항상 즉시 처리
            SecurityAlertDecision decision = ruleEngine.evaluateSecurityAlert(alert);
            
            // 위험도에 따른 즉시 대응
            switch (alert.getRiskLevel()) {
                case CRITICAL:
                    handleCriticalSecurityAlert(alert, decision);
                    break;
                case HIGH:
                    handleHighRiskSecurityAlert(alert, decision);
                    break;
                case MEDIUM:
                    handleMediumRiskSecurityAlert(alert, decision);
                    break;
                default:
                    handleLowRiskSecurityAlert(alert, decision);
            }
            
            // 보안 이벤트 로깅
            logSecurityEvent(alert);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("보안 알림 처리 중 오류 발생: {}", alert.getEventType(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 성능 관련 알림 처리
     */
    @Async("alertExecutor")
    public CompletableFuture<Void> processPerformanceAlert(PerformanceAlert alert) {
        try {
            log.info("성능 알림 처리 시작: {} (값: {})", 
                    alert.getMetricName(), alert.getCurrentValue());
            
            PerformanceAlertDecision decision = ruleEngine.evaluatePerformanceAlert(alert);
            
            if (decision.isTrendingUp() && decision.exceedsThreshold()) {
                // 성능 지표가 계속 악화되고 있는 경우
                sendPerformanceDegradationAlert(alert, decision);
                
                // 자동 스케일링 트리거 고려
                considerAutoScaling(alert, decision);
            }
            
            // 성능 메트릭 히스토리 저장
            savePerformanceHistory(alert);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("성능 알림 처리 중 오류 발생: {}", alert.getMetricName(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    // 헬퍼 메서드들

    /**
     * 알림 집계 및 중복 제거
     */
    private AlertSummary aggregateAlert(SystemAlert alert, AlertDecision decision) {
        // 최근 5분 내 동일한 알림이 있는지 확인
        List<AlertHistory> recentAlerts = alertHistoryRepository
                .findRecentAlertsByName(alert.getAlertName(), LocalDateTime.now().minusMinutes(5));
        
        if (!recentAlerts.isEmpty()) {
            // 기존 알림과 집계
            return AlertSummary.aggregate(alert, recentAlerts, decision);
        } else {
            // 새로운 알림
            return AlertSummary.fromAlert(alert, decision);
        }
    }

    /**
     * 심각도에 따른 알림 채널 선택
     */
    private List<AlertChannel> selectAlertChannels(AlertSeverity severity) {
        switch (severity) {
            case CRITICAL:
                return List.of(
                    AlertChannel.EMAIL_ONCALL,
                    AlertChannel.SMS,
                    AlertChannel.SLACK_URGENT,
                    AlertChannel.PAGERDUTY
                );
            case HIGH:
                return List.of(
                    AlertChannel.EMAIL_TEAM,
                    AlertChannel.SLACK_ALERTS
                );
            case MEDIUM:
                return List.of(
                    AlertChannel.SLACK_MONITORING
                );
            default:
                return List.of(
                    AlertChannel.LOG_ONLY
                );
        }
    }

    /**
     * 여러 채널로 알림 발송
     */
    private CompletableFuture<Void> sendToChannels(AlertSummary summary, List<AlertChannel> channels) {
        List<CompletableFuture<Boolean>> futures = channels.stream()
                .map(channel -> channelManager.sendAlert(summary, channel))
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * 중요 알림 즉시 발송
     */
    private void sendCriticalAlert(String title, String message) {
        try {
            CriticalAlert criticalAlert = CriticalAlert.builder()
                    .title(title)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .source("AlertService")
                    .build();
            
            // 모든 중요 채널로 즉시 발송
            channelManager.sendCriticalAlert(criticalAlert);
            
        } catch (Exception e) {
            // 최후의 수단: 시스템 로그에라도 기록
            log.error("중요 알림 발송 실패 - 제목: {}, 메시지: {}", title, message, e);
        }
    }

    /**
     * 에스컬레이션 스케줄링
     */
    private void scheduleEscalationIfNeeded(AlertSummary summary) {
        if (summary.getSeverity().ordinal() >= AlertSeverity.HIGH.ordinal()) {
            // 30분 후 에스컬레이션 체크
            CompletableFuture.delayedExecutor(30, java.util.concurrent.TimeUnit.MINUTES)
                    .execute(() -> checkEscalation(summary));
        }
    }

    /**
     * 에스컬레이션 체크
     */
    private void checkEscalation(AlertSummary summary) {
        // 알림이 해결되었는지 확인
        if (!isAlertResolved(summary)) {
            // 상위 담당자에게 에스컬레이션
            escalateAlert(summary);
        }
    }

    /**
     * 알림 해결 여부 확인
     */
    private boolean isAlertResolved(AlertSummary summary) {
        // 실제 구현에서는 메트릭 상태를 확인
        // 여기서는 간단히 구현
        return false;
    }

    /**
     * 알림 에스컬레이션
     */
    private void escalateAlert(AlertSummary summary) {
        EscalatedAlert escalated = EscalatedAlert.fromSummary(summary);
        escalated.setEscalationLevel(summary.getEscalationLevel() + 1);
        
        // 상위 관리자에게 알림
        channelManager.sendEscalatedAlert(escalated);
        
        log.warn("알림 에스컬레이션: {} (레벨: {})", 
                summary.getAlertName(), escalated.getEscalationLevel());
    }

    /**
     * 구조화된 알림 이벤트 로깅
     */
    private void logAlertEvent(AlertSummary summary) {
        Map<String, Object> context = new HashMap<>();
        context.put("alert_name", summary.getAlertName());
        context.put("severity", summary.getSeverity().name());
        context.put("count", summary.getCount());
        context.put("channels", summary.getChannels().size());
        
        structuredLogger.logBusinessEvent(
            com.alibou.booknetwork.logging.BusinessEvent.builder()
                .eventName("alert_processed")
                .entityType("alert")
                .entityId(summary.getAlertId())
                .action("notify")
                .status("success")
                .metadata(context)
                .build()
        );
    }

    // 보안 알림 처리 메서드들

    private void handleCriticalSecurityAlert(SecurityAlert alert, SecurityAlertDecision decision) {
        // 즉시 모든 보안 담당자에게 알림
        channelManager.sendSecurityAlert(alert, List.of(
            AlertChannel.EMAIL_SECURITY_TEAM,
            AlertChannel.SMS_SECURITY_ONCALL,
            AlertChannel.SLACK_SECURITY_CRITICAL
        ));
        
        // 자동 보안 조치 고려 (계정 잠금, IP 차단 등)
        if (decision.requiresAutomaticAction()) {
            triggerAutomaticSecurityAction(alert, decision);
        }
    }

    private void handleHighRiskSecurityAlert(SecurityAlert alert, SecurityAlertDecision decision) {
        channelManager.sendSecurityAlert(alert, List.of(
            AlertChannel.EMAIL_SECURITY_TEAM,
            AlertChannel.SLACK_SECURITY_HIGH
        ));
    }

    private void handleMediumRiskSecurityAlert(SecurityAlert alert, SecurityAlertDecision decision) {
        channelManager.sendSecurityAlert(alert, List.of(
            AlertChannel.SLACK_SECURITY_MEDIUM
        ));
    }

    private void handleLowRiskSecurityAlert(SecurityAlert alert, SecurityAlertDecision decision) {
        // 로그만 기록
        log.info("낮은 위험도 보안 이벤트: {}", alert.getEventType());
    }

    private void triggerAutomaticSecurityAction(SecurityAlert alert, SecurityAlertDecision decision) {
        // 자동 보안 조치 구현 (예: 계정 잠금, IP 차단)
        log.warn("자동 보안 조치 실행: {} for {}", decision.getAutomaticAction(), alert.getEventType());
    }

    // 성능 알림 처리 메서드들

    private void sendPerformanceDegradationAlert(PerformanceAlert alert, PerformanceAlertDecision decision) {
        PerformanceAlertSummary summary = PerformanceAlertSummary.builder()
                .metricName(alert.getMetricName())
                .currentValue(alert.getCurrentValue())
                .threshold(alert.getThreshold())
                .trend(decision.getTrend())
                .recommendation(decision.getRecommendation())
                .build();
        
        channelManager.sendPerformanceAlert(summary);
    }

    private void considerAutoScaling(PerformanceAlert alert, PerformanceAlertDecision decision) {
        if (decision.suggestsScaling()) {
            log.info("자동 스케일링 고려 중: {} (현재 값: {})", 
                    alert.getMetricName(), alert.getCurrentValue());
            
            // 실제 구현에서는 Kubernetes HPA/VPA 트리거
            // 여기서는 로그만 기록
        }
    }

    // 기타 헬퍼 메서드들

    private void sendUrgentBusinessAlert(BusinessAlert alert, BusinessAlertDecision decision) {
        // 긴급 비즈니스 알림 처리
        log.warn("긴급 비즈니스 알림: {}", alert.getEventType());
    }

    private void sendBusinessNotification(BusinessAlert alert, BusinessAlertDecision decision) {
        // 일반 비즈니스 알림 처리
        log.info("비즈니스 알림: {}", alert.getEventType());
    }

    private void updateBusinessMetrics(BusinessAlert alert) {
        // 비즈니스 메트릭 업데이트
    }

    private void logSecurityEvent(SecurityAlert alert) {
        // 보안 이벤트 로깅
        structuredLogger.logSecurityEvent(
            com.alibou.booknetwork.logging.SecurityEvent.builder()
                .eventName(alert.getEventType())
                .riskLevel(alert.getRiskLevel().name().toLowerCase())
                .result("alert_triggered")
                .build()
        );
    }

    private void saveAlertHistory(AlertSummary summary) {
        // 알림 히스토리 저장 구현
    }

    private void savePerformanceHistory(PerformanceAlert alert) {
        // 성능 히스토리 저장 구현
    }
}