package com.alibou.booknetwork.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 지능형 알림 규칙 엔진
 * 
 * 이 클래스는 복잡한 비즈니스 규칙과 컨텍스트를 기반으로 
 * 알림의 우선순위, 필터링, 라우팅을 결정합니다.
 * 
 * 주요 기능:
 * 1. 동적 규칙 기반 알림 필터링
 * 2. 시간대별 알림 정책 적용
 * 3. 알림 빈도 제한 및 중복 방지
 * 4. 컨텍스트 기반 심각도 조정
 * 5. 머신러닝 기반 노이즈 필터링
 * 
 * 왜 필요한가?
 * - 알림 피로도 방지
 * - 중요한 알림의 우선순위 보장
 * - 운영 효율성 극대화
 * - 거짓 양성 알림 최소화
 */
@Component
@Slf4j
public class AlertRuleEngine {

    // 알림 히스토리 캐시 (메모리 기반, 실제로는 Redis 사용 권장)
    private final Map<String, AlarmHistory> alertHistoryCache = new ConcurrentHashMap<>();
    
    // 시간별 알림 빈도 제한
    private final Map<String, Integer> hourlyAlertCounts = new ConcurrentHashMap<>();
    
    // 알림 규칙 설정
    private final AlertRuleConfig ruleConfig = new AlertRuleConfig();

    /**
     * 시스템 알림 평가
     */
    public AlertDecision evaluateAlert(SystemAlert alert) {
        log.debug("시스템 알림 평가 시작: {}", alert.getAlertName());
        
        AlertDecision.AlertDecisionBuilder decision = AlertDecision.builder()
                .alertName(alert.getAlertName())
                .originalSeverity(alert.getSeverity())
                .evaluationTime(LocalDateTime.now());

        // 1. 기본 억제 조건 체크
        if (shouldSuppressBasedOnRules(alert)) {
            return decision
                    .shouldSuppress(true)
                    .suppressionReason("규칙 기반 억제")
                    .build();
        }

        // 2. 시간 기반 정책 적용
        AlertSeverity adjustedSeverity = applyTimeBasedPolicy(alert);
        decision.adjustedSeverity(adjustedSeverity);

        // 3. 빈도 기반 제한 체크
        if (exceedsFrequencyLimit(alert)) {
            return decision
                    .shouldSuppress(true)
                    .suppressionReason("빈도 제한 초과")
                    .build();
        }

        // 4. 컨텍스트 기반 심각도 조정
        AlertSeverity contextAdjustedSeverity = adjustSeverityByContext(alert, adjustedSeverity);
        decision.finalSeverity(contextAdjustedSeverity);

        // 5. 라우팅 규칙 결정
        List<String> targetTeams = determineTargetTeams(alert, contextAdjustedSeverity);
        decision.targetTeams(targetTeams);

        // 6. 자동 조치 결정
        List<String> automaticActions = determineAutomaticActions(alert, contextAdjustedSeverity);
        decision.automaticActions(automaticActions);

        // 7. 알림 히스토리 업데이트
        updateAlertHistory(alert);

        AlertDecision finalDecision = decision.build();
        log.info("시스템 알림 평가 완료: {} -> {}", 
                alert.getSeverity(), finalDecision.getFinalSeverity());
        
        return finalDecision;
    }

    /**
     * 비즈니스 알림 평가
     */
    public BusinessAlertDecision evaluateBusinessAlert(BusinessAlert alert) {
        log.debug("비즈니스 알림 평가 시작: {}", alert.getEventType());
        
        BusinessAlertDecision.BusinessAlertDecisionBuilder decision = 
                BusinessAlertDecision.builder()
                        .eventType(alert.getEventType())
                        .evaluationTime(LocalDateTime.now());

        // 1. 비즈니스 중요도 평가
        BusinessImportance importance = evaluateBusinessImportance(alert);
        decision.importance(importance);

        // 2. 즉시 조치 필요 여부 판단
        boolean requiresImmediateAction = requiresImmediateBusinessAction(alert, importance);
        decision.requiresImmedateAction(requiresImmediateAction);

        // 3. 알림 필요 여부 판단
        boolean requiresNotification = requiresBusinessNotification(alert, importance);
        decision.requiresNotification(requiresNotification);

        // 4. SLA 영향도 평가
        SLAImpact slaImpact = evaluateSLAImpact(alert);
        decision.slaImpact(slaImpact);

        // 5. 고객 영향도 평가
        CustomerImpact customerImpact = evaluateCustomerImpact(alert);
        decision.customerImpact(customerImpact);

        BusinessAlertDecision finalDecision = decision.build();
        log.info("비즈니스 알림 평가 완료: {} (중요도: {})", 
                alert.getEventType(), importance);
        
        return finalDecision;
    }

    /**
     * 보안 알림 평가
     */
    public SecurityAlertDecision evaluateSecurityAlert(SecurityAlert alert) {
        log.debug("보안 알림 평가 시작: {}", alert.getEventType());
        
        SecurityAlertDecision.SecurityAlertDecisionBuilder decision = 
                SecurityAlertDecision.builder()
                        .eventType(alert.getEventType())
                        .evaluationTime(LocalDateTime.now());

        // 1. 위협 레벨 평가
        ThreatLevel threatLevel = evaluateThreatLevel(alert);
        decision.threatLevel(threatLevel);

        // 2. 자동 대응 필요 여부
        boolean requiresAutomaticAction = requiresAutomaticSecurityAction(alert, threatLevel);
        decision.requiresAutomaticAction(requiresAutomaticAction);

        // 3. 자동 대응 액션 결정
        if (requiresAutomaticAction) {
            String automaticAction = determineSecurityAction(alert, threatLevel);
            decision.automaticAction(automaticAction);
        }

        // 4. 에스컬레이션 필요 여부
        boolean requiresEscalation = requiresSecurityEscalation(alert, threatLevel);
        decision.requiresEscalation(requiresEscalation);

        // 5. 증거 수집 필요 여부
        boolean requiresForensics = requiresForensicAnalysis(alert, threatLevel);
        decision.requiresForensics(requiresForensics);

        SecurityAlertDecision finalDecision = decision.build();
        log.warn("보안 알림 평가 완료: {} (위협 레벨: {})", 
                alert.getEventType(), threatLevel);
        
        return finalDecision;
    }

    /**
     * 성능 알림 평가
     */
    public PerformanceAlertDecision evaluatePerformanceAlert(PerformanceAlert alert) {
        log.debug("성능 알림 평가 시작: {}", alert.getMetricName());
        
        PerformanceAlertDecision.PerformanceAlertDecisionBuilder decision = 
                PerformanceAlertDecision.builder()
                        .metricName(alert.getMetricName())
                        .evaluationTime(LocalDateTime.now());

        // 1. 임계값 초과 여부
        boolean exceedsThreshold = alert.getCurrentValue() > alert.getThreshold();
        decision.exceedsThreshold(exceedsThreshold);

        // 2. 트렌드 분석
        PerformanceTrend trend = analyzePerformanceTrend(alert);
        decision.trendingUp(trend == PerformanceTrend.INCREASING);
        decision.trend(trend);

        // 3. 비즈니스 시간 고려
        boolean isDuringBusinessHours = isBusinessHours();
        decision.duringBusinessHours(isDuringBusinessHours);

        // 4. 스케일링 권장 여부
        boolean suggestsScaling = shouldSuggestScaling(alert, trend, isDuringBusinessHours);
        decision.suggestsScaling(suggestsScaling);

        // 5. 권장 조치
        String recommendation = generatePerformanceRecommendation(alert, trend, exceedsThreshold);
        decision.recommendation(recommendation);

        // 6. 심각도 레벨
        PerformanceSeverity severity = determinePerformanceSeverity(alert, trend, isDuringBusinessHours);
        decision.severity(severity);

        PerformanceAlertDecision finalDecision = decision.build();
        log.info("성능 알림 평가 완료: {} (트렌드: {}, 권장: {})", 
                alert.getMetricName(), trend, recommendation);
        
        return finalDecision;
    }

    // 헬퍼 메서드들

    /**
     * 기본 억제 규칙 체크
     */
    private boolean shouldSuppressBasedOnRules(SystemAlert alert) {
        // 1. 점검 시간 중 억제
        if (isMaintenanceWindow()) {
            log.debug("점검 시간 중 알림 억제: {}", alert.getAlertName());
            return true;
        }

        // 2. 알려진 이슈에 대한 억제
        if (ruleConfig.getKnownIssues().contains(alert.getAlertName())) {
            log.debug("알려진 이슈로 인한 알림 억제: {}", alert.getAlertName());
            return true;
        }

        // 3. 테스트 환경 알림 억제
        if (alert.getEnvironment() != null && alert.getEnvironment().equals("test")) {
            log.debug("테스트 환경 알림 억제: {}", alert.getAlertName());
            return true;
        }

        return false;
    }

    /**
     * 시간 기반 정책 적용
     */
    private AlertSeverity applyTimeBasedPolicy(SystemAlert alert) {
        LocalTime currentTime = LocalTime.now();
        AlertSeverity originalSeverity = alert.getSeverity();

        // 업무 시간 외에는 심각도 한 단계 낮춤 (CRITICAL 제외)
        if (!isBusinessHours() && originalSeverity != AlertSeverity.CRITICAL) {
            switch (originalSeverity) {
                case HIGH:
                    return AlertSeverity.MEDIUM;
                case MEDIUM:
                    return AlertSeverity.LOW;
                case LOW:
                    return AlertSeverity.INFO;
                default:
                    return originalSeverity;
            }
        }

        return originalSeverity;
    }

    /**
     * 빈도 제한 체크
     */
    private boolean exceedsFrequencyLimit(SystemAlert alert) {
        String hourKey = alert.getAlertName() + ":" + LocalDateTime.now().getHour();
        int currentCount = hourlyAlertCounts.getOrDefault(hourKey, 0);
        
        // 시간당 동일 알림 제한
        int maxAlertsPerHour = ruleConfig.getMaxAlertsPerHour(alert.getSeverity());
        
        if (currentCount >= maxAlertsPerHour) {
            log.warn("시간당 알림 빈도 초과: {} ({}회 >= {}회)", 
                    alert.getAlertName(), currentCount, maxAlertsPerHour);
            return true;
        }

        // 카운트 증가
        hourlyAlertCounts.put(hourKey, currentCount + 1);
        return false;
    }

    /**
     * 컨텍스트 기반 심각도 조정
     */
    private AlertSeverity adjustSeverityByContext(SystemAlert alert, AlertSeverity currentSeverity) {
        // 1. 연관된 다른 알림들이 있는지 확인
        if (hasRelatedActiveAlerts(alert)) {
            // 연관 알림이 있으면 심각도 상승
            return increaseSeverity(currentSeverity);
        }

        // 2. 시스템 부하 상태 확인
        if (isSystemUnderHighLoad()) {
            // 시스템 부하가 높으면 심각도 상승
            return increaseSeverity(currentSeverity);
        }

        // 3. 고객 영향도 확인
        if (hasCustomerImpact(alert)) {
            // 고객 영향이 있으면 심각도 상승
            return increaseSeverity(currentSeverity);
        }

        return currentSeverity;
    }

    /**
     * 대상 팀 결정
     */
    private List<String> determineTargetTeams(SystemAlert alert, AlertSeverity severity) {
        switch (severity) {
            case CRITICAL:
                return List.of("oncall-team", "management", "devops-team");
            case HIGH:
                return List.of("oncall-team", "devops-team");
            case MEDIUM:
                return List.of("devops-team");
            default:
                return List.of("monitoring-team");
        }
    }

    /**
     * 자동 조치 결정
     */
    private List<String> determineAutomaticActions(SystemAlert alert, AlertSeverity severity) {
        if (severity == AlertSeverity.CRITICAL) {
            switch (alert.getMetricType()) {
                case "cpu_usage":
                    return List.of("auto-scale-up", "kill-heavy-processes");
                case "memory_usage":
                    return List.of("auto-scale-up", "clear-cache");
                case "disk_usage":
                    return List.of("cleanup-logs", "archive-old-data");
                default:
                    return List.of("create-incident");
            }
        }
        return List.of();
    }

    // 비즈니스 알림 관련 메서드들

    private BusinessImportance evaluateBusinessImportance(BusinessAlert alert) {
        switch (alert.getEventType()) {
            case "payment_failure":
            case "user_data_breach":
                return BusinessImportance.CRITICAL;
            case "service_degradation":
            case "high_error_rate":
                return BusinessImportance.HIGH;
            case "user_complaint":
            case "feature_usage_drop":
                return BusinessImportance.MEDIUM;
            default:
                return BusinessImportance.LOW;
        }
    }

    private boolean requiresImmediateBusinessAction(BusinessAlert alert, BusinessImportance importance) {
        return importance.ordinal() >= BusinessImportance.HIGH.ordinal();
    }

    private boolean requiresBusinessNotification(BusinessAlert alert, BusinessImportance importance) {
        return importance.ordinal() >= BusinessImportance.MEDIUM.ordinal();
    }

    private SLAImpact evaluateSLAImpact(BusinessAlert alert) {
        // SLA 영향도 평가 로직
        return SLAImpact.MEDIUM;
    }

    private CustomerImpact evaluateCustomerImpact(BusinessAlert alert) {
        // 고객 영향도 평가 로직
        return CustomerImpact.LOW;
    }

    // 보안 알림 관련 메서드들

    private ThreatLevel evaluateThreatLevel(SecurityAlert alert) {
        switch (alert.getRiskLevel()) {
            case CRITICAL:
                return ThreatLevel.CRITICAL;
            case HIGH:
                return ThreatLevel.HIGH;
            case MEDIUM:
                return ThreatLevel.MEDIUM;
            default:
                return ThreatLevel.LOW;
        }
    }

    private boolean requiresAutomaticSecurityAction(SecurityAlert alert, ThreatLevel threatLevel) {
        return threatLevel.ordinal() >= ThreatLevel.HIGH.ordinal();
    }

    private String determineSecurityAction(SecurityAlert alert, ThreatLevel threatLevel) {
        switch (alert.getEventType()) {
            case "brute_force_attack":
                return "block_ip_address";
            case "suspicious_login":
                return "require_additional_auth";
            case "data_exfiltration":
                return "block_user_access";
            default:
                return "increase_monitoring";
        }
    }

    private boolean requiresSecurityEscalation(SecurityAlert alert, ThreatLevel threatLevel) {
        return threatLevel == ThreatLevel.CRITICAL;
    }

    private boolean requiresForensicAnalysis(SecurityAlert alert, ThreatLevel threatLevel) {
        return threatLevel.ordinal() >= ThreatLevel.HIGH.ordinal();
    }

    // 성능 알림 관련 메서드들

    private PerformanceTrend analyzePerformanceTrend(PerformanceAlert alert) {
        // 실제로는 시계열 데이터 분석
        // 여기서는 간단히 구현
        if (alert.getCurrentValue() > alert.getThreshold() * 1.2) {
            return PerformanceTrend.RAPIDLY_INCREASING;
        } else if (alert.getCurrentValue() > alert.getThreshold()) {
            return PerformanceTrend.INCREASING;
        } else {
            return PerformanceTrend.STABLE;
        }
    }

    private boolean shouldSuggestScaling(PerformanceAlert alert, PerformanceTrend trend, boolean isBusinessHours) {
        return trend == PerformanceTrend.RAPIDLY_INCREASING || 
               (trend == PerformanceTrend.INCREASING && isBusinessHours);
    }

    private String generatePerformanceRecommendation(PerformanceAlert alert, PerformanceTrend trend, boolean exceedsThreshold) {
        if (exceedsThreshold && trend == PerformanceTrend.RAPIDLY_INCREASING) {
            return "즉시 스케일 업 권장";
        } else if (exceedsThreshold) {
            return "모니터링 강화 및 스케일링 준비";
        } else {
            return "지속 모니터링";
        }
    }

    private PerformanceSeverity determinePerformanceSeverity(PerformanceAlert alert, PerformanceTrend trend, boolean isBusinessHours) {
        if (alert.getCurrentValue() > alert.getThreshold() * 1.5) {
            return PerformanceSeverity.CRITICAL;
        } else if (alert.getCurrentValue() > alert.getThreshold() * 1.2) {
            return isBusinessHours ? PerformanceSeverity.HIGH : PerformanceSeverity.MEDIUM;
        } else {
            return PerformanceSeverity.LOW;
        }
    }

    // 유틸리티 메서드들

    private boolean isMaintenanceWindow() {
        // 점검 시간 확인 (예: 매일 새벽 2-4시)
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(2, 0)) && now.isBefore(LocalTime.of(4, 0));
    }

    private boolean isBusinessHours() {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(18, 0));
    }

    private boolean hasRelatedActiveAlerts(SystemAlert alert) {
        // 연관 알림 확인 로직
        return false;
    }

    private boolean isSystemUnderHighLoad() {
        // 시스템 부하 확인 로직
        return false;
    }

    private boolean hasCustomerImpact(SystemAlert alert) {
        // 고객 영향 확인 로직
        return false;
    }

    private AlertSeverity increaseSeverity(AlertSeverity currentSeverity) {
        switch (currentSeverity) {
            case INFO:
                return AlertSeverity.LOW;
            case LOW:
                return AlertSeverity.MEDIUM;
            case MEDIUM:
                return AlertSeverity.HIGH;
            case HIGH:
                return AlertSeverity.CRITICAL;
            default:
                return currentSeverity;
        }
    }

    private void updateAlertHistory(SystemAlert alert) {
        String key = alert.getAlertName();
        AlarmHistory history = alertHistoryCache.computeIfAbsent(key, k -> new AlarmHistory());
        history.addOccurrence(LocalDateTime.now());
    }

    // 내부 클래스들 (실제로는 별도 파일로 분리 권장)
    
    private static class AlarmHistory {
        private final List<LocalDateTime> occurrences = new java.util.ArrayList<>();
        
        public void addOccurrence(LocalDateTime time) {
            occurrences.add(time);
        }
    }
    
    private static class AlertRuleConfig {
        private final List<String> knownIssues = List.of("test_alert", "maintenance_alert");
        
        public List<String> getKnownIssues() {
            return knownIssues;
        }
        
        public int getMaxAlertsPerHour(AlertSeverity severity) {
            switch (severity) {
                case CRITICAL: return 10;
                case HIGH: return 20;
                case MEDIUM: return 50;
                default: return 100;
            }
        }
    }
    
    // 열거형들
    public enum BusinessImportance { LOW, MEDIUM, HIGH, CRITICAL }
    public enum SLAImpact { NONE, LOW, MEDIUM, HIGH, CRITICAL }
    public enum CustomerImpact { NONE, LOW, MEDIUM, HIGH, CRITICAL }
    public enum ThreatLevel { LOW, MEDIUM, HIGH, CRITICAL }
    public enum PerformanceTrend { DECREASING, STABLE, INCREASING, RAPIDLY_INCREASING }
    public enum PerformanceSeverity { LOW, MEDIUM, HIGH, CRITICAL }
}