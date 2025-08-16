package com.alibou.booknetwork.sla;

import com.alibou.booknetwork.logging.StructuredLogger;
import com.alibou.booknetwork.metrics.BusinessMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * SLA/SLO 모니터링 서비스
 * 
 * 이 클래스는 서비스 수준 목표(SLO)와 서비스 수준 협약(SLA)을 실시간으로 모니터링하고
 * 목표 달성 여부를 추적합니다.
 * 
 * 주요 기능:
 * 1. 가용성(Availability) SLO 모니터링 (99.9% 목표)
 * 2. 응답 시간(Latency) SLO 모니터링 (95% < 500ms)
 * 3. 에러율(Error Rate) SLO 모니터링 (< 0.1%)
 * 4. 처리량(Throughput) SLO 모니터링
 * 5. 에러 버짓(Error Budget) 관리
 * 6. SLA 위반 알림 및 보고
 * 
 * SLA vs SLO:
 * - SLA (Service Level Agreement): 고객과의 계약, 위반 시 법적/재정적 책임
 * - SLO (Service Level Objective): 내부 목표, SLA 달성을 위한 구체적 수치
 * - SLI (Service Level Indicator): 실제 측정값
 * 
 * Error Budget:
 * - 허용 가능한 장애 시간/비율
 * - 99.9% 가용성 = 월 43.2분의 장애 허용
 * - 예산 소진 시 새 기능 배포 중단, 안정성 우선
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SLAMonitoringService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StructuredLogger structuredLogger;
    private final BusinessMetricsCollector metricsCollector;
    private final SLAConfigurationProperties slaConfig;

    // SLA 메트릭 키 상수
    private static final String SLA_METRICS_PREFIX = "sla:metrics:";
    private static final String ERROR_BUDGET_PREFIX = "sla:error_budget:";
    private static final String SLO_VIOLATIONS_PREFIX = "sla:violations:";

    /**
     * 실시간 SLA 메트릭 수집 (매분 실행)
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void collectSLAMetrics() {
        LocalDateTime now = LocalDateTime.now();
        String timeKey = now.truncatedTo(ChronoUnit.MINUTES).toString();

        try {
            log.debug("SLA 메트릭 수집 시작: {}", timeKey);

            // 1. 가용성 메트릭 수집
            collectAvailabilityMetrics(timeKey);

            // 2. 응답 시간 메트릭 수집
            collectLatencyMetrics(timeKey);

            // 3. 에러율 메트릭 수집
            collectErrorRateMetrics(timeKey);

            // 4. 처리량 메트릭 수집
            collectThroughputMetrics(timeKey);

            // 5. 비즈니스 메트릭 수집
            collectBusinessMetrics(timeKey);

            log.debug("SLA 메트릭 수집 완료: {}", timeKey);

        } catch (Exception e) {
            log.error("SLA 메트릭 수집 중 오류 발생", e);
        }
    }

    /**
     * SLO 평가 및 위반 체크 (매 5분마다)
     */
    @Scheduled(fixedRate = 300000) // 5분마다
    public void evaluateSLOs() {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            log.info("SLO 평가 시작: {}", now);

            // 각 SLO별 평가
            for (SLODefinition slo : slaConfig.getSloDefinitions()) {
                evaluateSingleSLO(slo, now);
            }

            // 에러 버짓 업데이트
            updateErrorBudgets(now);

            // SLA 대시보드 데이터 업데이트
            updateDashboardData(now);

            log.info("SLO 평가 완료: {}", now);

        } catch (Exception e) {
            log.error("SLO 평가 중 오류 발생", e);
        }
    }

    /**
     * 일일 SLA 보고서 생성 (매일 자정)
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    public void generateDailySLAReport() {
        LocalDateTime reportDate = LocalDateTime.now().minusDays(1);
        
        try {
            log.info("일일 SLA 보고서 생성 시작: {}", reportDate.toLocalDate());

            SLAReport dailyReport = SLAReport.builder()
                    .reportDate(reportDate.toLocalDate())
                    .reportType(SLAReportType.DAILY)
                    .generatedAt(LocalDateTime.now())
                    .build();

            // 각 SLO별 일일 결과 수집
            Map<String, SLOResult> sloResults = new HashMap<>();
            for (SLODefinition slo : slaConfig.getSloDefinitions()) {
                SLOResult result = calculateDailySLOResult(slo, reportDate);
                sloResults.put(slo.getName(), result);
            }
            dailyReport.setSloResults(sloResults);

            // 에러 버짓 상태 계산
            Map<String, ErrorBudgetStatus> errorBudgets = calculateErrorBudgetStatus(reportDate);
            dailyReport.setErrorBudgetStatus(errorBudgets);

            // 보고서 저장 및 발송
            saveSLAReport(dailyReport);
            sendSLAReport(dailyReport);

            log.info("일일 SLA 보고서 생성 완료: {}", reportDate.toLocalDate());

        } catch (Exception e) {
            log.error("일일 SLA 보고서 생성 중 오류 발생", e);
        }
    }

    /**
     * 가용성 메트릭 수집
     */
    private void collectAvailabilityMetrics(String timeKey) {
        // 현재 서비스 상태 확인
        boolean isServiceUp = checkServiceAvailability();
        
        String availabilityKey = SLA_METRICS_PREFIX + "availability:" + timeKey;
        redisTemplate.opsForValue().set(availabilityKey, isServiceUp ? 1 : 0, 7, TimeUnit.DAYS);
        
        // 가용성 통계 업데이트
        String dailyAvailabilityKey = "sla:daily_availability:" + timeKey.substring(0, 10);
        if (isServiceUp) {
            redisTemplate.opsForValue().increment(dailyAvailabilityKey);
        }
        redisTemplate.expire(dailyAvailabilityKey, 30, TimeUnit.DAYS);
        
        log.trace("가용성 메트릭 수집됨: {} = {}", timeKey, isServiceUp);
    }

    /**
     * 응답 시간 메트릭 수집
     */
    private void collectLatencyMetrics(String timeKey) {
        // 현재 응답 시간 통계 가져오기 (실제로는 메트릭 시스템에서 조회)
        LatencyStatistics latencyStats = getCurrentLatencyStatistics();
        
        String latencyKey = SLA_METRICS_PREFIX + "latency:" + timeKey;
        Map<String, Object> latencyData = Map.of(
            "p50", latencyStats.getP50(),
            "p95", latencyStats.getP95(),
            "p99", latencyStats.getP99(),
            "average", latencyStats.getAverage()
        );
        
        redisTemplate.opsForHash().putAll(latencyKey, latencyData);
        redisTemplate.expire(latencyKey, 7, TimeUnit.DAYS);
        
        log.trace("응답 시간 메트릭 수집됨: {} = P95:{}ms", timeKey, latencyStats.getP95());
    }

    /**
     * 에러율 메트릭 수집
     */
    private void collectErrorRateMetrics(String timeKey) {
        // 현재 에러율 통계 가져오기
        ErrorRateStatistics errorStats = getCurrentErrorRateStatistics();
        
        String errorRateKey = SLA_METRICS_PREFIX + "error_rate:" + timeKey;
        redisTemplate.opsForValue().set(errorRateKey, errorStats.getErrorRate(), 7, TimeUnit.DAYS);
        
        // 에러 카운트도 저장
        String errorCountKey = SLA_METRICS_PREFIX + "error_count:" + timeKey;
        Map<String, Object> errorData = Map.of(
            "total_requests", errorStats.getTotalRequests(),
            "error_requests", errorStats.getErrorRequests(),
            "error_rate", errorStats.getErrorRate()
        );
        
        redisTemplate.opsForHash().putAll(errorCountKey, errorData);
        redisTemplate.expire(errorCountKey, 7, TimeUnit.DAYS);
        
        log.trace("에러율 메트릭 수집됨: {} = {}%", timeKey, errorStats.getErrorRate());
    }

    /**
     * 처리량 메트릭 수집
     */
    private void collectThroughputMetrics(String timeKey) {
        // 현재 처리량 통계 가져오기
        ThroughputStatistics throughputStats = getCurrentThroughputStatistics();
        
        String throughputKey = SLA_METRICS_PREFIX + "throughput:" + timeKey;
        redisTemplate.opsForValue().set(throughputKey, throughputStats.getRequestsPerMinute(), 7, TimeUnit.DAYS);
        
        log.trace("처리량 메트릭 수집됨: {} = {} req/min", timeKey, throughputStats.getRequestsPerMinute());
    }

    /**
     * 비즈니스 메트릭 수집
     */
    private void collectBusinessMetrics(String timeKey) {
        // 비즈니스 특화 메트릭 수집
        BusinessMetrics businessMetrics = getCurrentBusinessMetrics();
        
        String businessKey = SLA_METRICS_PREFIX + "business:" + timeKey;
        Map<String, Object> businessData = Map.of(
            "book_registrations", businessMetrics.getBookRegistrations(),
            "user_logins", businessMetrics.getUserLogins(),
            "book_borrows", businessMetrics.getBookBorrows(),
            "active_users", businessMetrics.getActiveUsers()
        );
        
        redisTemplate.opsForHash().putAll(businessKey, businessData);
        redisTemplate.expire(businessKey, 7, TimeUnit.DAYS);
        
        log.trace("비즈니스 메트릭 수집됨: {}", timeKey);
    }

    /**
     * 개별 SLO 평가
     */
    private void evaluateSingleSLO(SLODefinition slo, LocalDateTime evaluationTime) {
        try {
            log.debug("SLO 평가 시작: {}", slo.getName());

            // 평가 기간 데이터 수집
            LocalDateTime startTime = evaluationTime.minus(slo.getEvaluationWindowMinutes(), ChronoUnit.MINUTES);
            SLOEvaluationResult result = calculateSLOResult(slo, startTime, evaluationTime);

            // SLO 위반 체크
            if (result.getCurrentValue() < slo.getTargetValue()) {
                handleSLOViolation(slo, result, evaluationTime);
            } else {
                log.debug("SLO 목표 달성: {} = {}% (목표: {}%)", 
                         slo.getName(), result.getCurrentValue(), slo.getTargetValue());
            }

            // 결과 저장
            saveSLOEvaluationResult(slo, result, evaluationTime);

        } catch (Exception e) {
            log.error("SLO 평가 중 오류 발생: {}", slo.getName(), e);
        }
    }

    /**
     * SLO 결과 계산
     */
    private SLOEvaluationResult calculateSLOResult(SLODefinition slo, LocalDateTime startTime, LocalDateTime endTime) {
        switch (slo.getType()) {
            case AVAILABILITY:
                return calculateAvailabilitySLO(slo, startTime, endTime);
            case LATENCY:
                return calculateLatencySLO(slo, startTime, endTime);
            case ERROR_RATE:
                return calculateErrorRateSLO(slo, startTime, endTime);
            case THROUGHPUT:
                return calculateThroughputSLO(slo, startTime, endTime);
            default:
                throw new IllegalArgumentException("지원하지 않는 SLO 타입: " + slo.getType());
        }
    }

    /**
     * 가용성 SLO 계산
     */
    private SLOEvaluationResult calculateAvailabilitySLO(SLODefinition slo, LocalDateTime startTime, LocalDateTime endTime) {
        // Redis에서 가용성 데이터 조회
        List<Integer> availabilityData = getAvailabilityDataFromRedis(startTime, endTime);
        
        if (availabilityData.isEmpty()) {
            return SLOEvaluationResult.builder()
                    .sloName(slo.getName())
                    .currentValue(0.0)
                    .targetValue(slo.getTargetValue())
                    .status(SLOStatus.INSUFFICIENT_DATA)
                    .build();
        }

        // 가용성 계산 (업타임 / 전체 시간)
        long upMinutes = availabilityData.stream().mapToLong(Integer::longValue).sum();
        long totalMinutes = availabilityData.size();
        double availabilityPercentage = (double) upMinutes / totalMinutes * 100;

        return SLOEvaluationResult.builder()
                .sloName(slo.getName())
                .currentValue(availabilityPercentage)
                .targetValue(slo.getTargetValue())
                .status(availabilityPercentage >= slo.getTargetValue() ? SLOStatus.MEETING : SLOStatus.VIOLATING)
                .dataPoints(availabilityData.size())
                .evaluationPeriod(java.time.Duration.between(startTime, endTime))
                .build();
    }

    /**
     * 응답 시간 SLO 계산
     */
    private SLOEvaluationResult calculateLatencySLO(SLODefinition slo, LocalDateTime startTime, LocalDateTime endTime) {
        // Redis에서 응답 시간 데이터 조회
        List<Map<String, Object>> latencyData = getLatencyDataFromRedis(startTime, endTime);
        
        if (latencyData.isEmpty()) {
            return SLOEvaluationResult.builder()
                    .sloName(slo.getName())
                    .currentValue(0.0)
                    .targetValue(slo.getTargetValue())
                    .status(SLOStatus.INSUFFICIENT_DATA)
                    .build();
        }

        // P95 응답 시간이 목표치 이하인 비율 계산
        long meetingTarget = latencyData.stream()
                .mapToLong(data -> {
                    Double p95 = (Double) data.get("p95");
                    return p95 != null && p95 <= slo.getThresholdValue() ? 1 : 0;
                })
                .sum();

        double successRate = (double) meetingTarget / latencyData.size() * 100;

        return SLOEvaluationResult.builder()
                .sloName(slo.getName())
                .currentValue(successRate)
                .targetValue(slo.getTargetValue())
                .status(successRate >= slo.getTargetValue() ? SLOStatus.MEETING : SLOStatus.VIOLATING)
                .dataPoints(latencyData.size())
                .evaluationPeriod(java.time.Duration.between(startTime, endTime))
                .build();
    }

    /**
     * 에러율 SLO 계산
     */
    private SLOEvaluationResult calculateErrorRateSLO(SLODefinition slo, LocalDateTime startTime, LocalDateTime endTime) {
        // Redis에서 에러율 데이터 조회
        List<Double> errorRates = getErrorRateDataFromRedis(startTime, endTime);
        
        if (errorRates.isEmpty()) {
            return SLOEvaluationResult.builder()
                    .sloName(slo.getName())
                    .currentValue(100.0) // 데이터 없음 = 100% 성공률로 가정
                    .targetValue(slo.getTargetValue())
                    .status(SLOStatus.INSUFFICIENT_DATA)
                    .build();
        }

        // 평균 에러율 계산
        double averageErrorRate = errorRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double successRate = 100.0 - averageErrorRate;

        return SLOEvaluationResult.builder()
                .sloName(slo.getName())
                .currentValue(successRate)
                .targetValue(slo.getTargetValue())
                .status(successRate >= slo.getTargetValue() ? SLOStatus.MEETING : SLOStatus.VIOLATING)
                .dataPoints(errorRates.size())
                .evaluationPeriod(java.time.Duration.between(startTime, endTime))
                .build();
    }

    /**
     * 처리량 SLO 계산
     */
    private SLOEvaluationResult calculateThroughputSLO(SLODefinition slo, LocalDateTime startTime, LocalDateTime endTime) {
        // Redis에서 처리량 데이터 조회
        List<Double> throughputData = getThroughputDataFromRedis(startTime, endTime);
        
        if (throughputData.isEmpty()) {
            return SLOEvaluationResult.builder()
                    .sloName(slo.getName())
                    .currentValue(0.0)
                    .targetValue(slo.getTargetValue())
                    .status(SLOStatus.INSUFFICIENT_DATA)
                    .build();
        }

        // 평균 처리량 계산
        double averageThroughput = throughputData.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        return SLOEvaluationResult.builder()
                .sloName(slo.getName())
                .currentValue(averageThroughput)
                .targetValue(slo.getTargetValue())
                .status(averageThroughput >= slo.getTargetValue() ? SLOStatus.MEETING : SLOStatus.VIOLATING)
                .dataPoints(throughputData.size())
                .evaluationPeriod(java.time.Duration.between(startTime, endTime))
                .build();
    }

    /**
     * SLO 위반 처리
     */
    private void handleSLOViolation(SLODefinition slo, SLOEvaluationResult result, LocalDateTime violationTime) {
        log.warn("SLO 위반 감지: {} = {}% (목표: {}%)", 
                slo.getName(), result.getCurrentValue(), slo.getTargetValue());

        // 위반 이벤트 저장
        SLOViolation violation = SLOViolation.builder()
                .sloName(slo.getName())
                .violationTime(violationTime)
                .currentValue(result.getCurrentValue())
                .targetValue(slo.getTargetValue())
                .severity(determineSeverity(slo, result))
                .build();

        saveSLOViolation(violation);

        // 알림 발송
        sendSLOViolationAlert(violation);

        // 구조화된 로깅
        logSLOViolation(violation);
    }

    /**
     * 에러 버짓 업데이트
     */
    private void updateErrorBudgets(LocalDateTime updateTime) {
        for (SLODefinition slo : slaConfig.getSloDefinitions()) {
            if (slo.getType() == SLOType.AVAILABILITY || slo.getType() == SLOType.ERROR_RATE) {
                updateSingleErrorBudget(slo, updateTime);
            }
        }
    }

    /**
     * 개별 에러 버짓 업데이트
     */
    private void updateSingleErrorBudget(SLODefinition slo, LocalDateTime updateTime) {
        // 월별 에러 버짓 계산
        LocalDateTime monthStart = updateTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        // 현재까지의 SLO 달성률 계산
        SLOEvaluationResult monthlyResult = calculateSLOResult(slo, monthStart, updateTime);
        
        // 에러 버짓 계산 (허용 가능한 에러율 = 100 - SLO 목표치)
        double allowedErrorRate = 100.0 - slo.getTargetValue();
        double actualErrorRate = 100.0 - monthlyResult.getCurrentValue();
        
        // 에러 버짓 소진율 계산
        double budgetConsumptionRate = actualErrorRate / allowedErrorRate;
        double remainingBudgetPercent = Math.max(0, (1.0 - budgetConsumptionRate) * 100);
        
        // 에러 버짓 상태 저장
        ErrorBudgetStatus budgetStatus = ErrorBudgetStatus.builder()
                .sloName(slo.getName())
                .totalBudget(allowedErrorRate)
                .consumedBudget(actualErrorRate)
                .remainingBudgetPercent(remainingBudgetPercent)
                .isExhausted(remainingBudgetPercent <= 0)
                .lastUpdated(updateTime)
                .build();

        saveErrorBudgetStatus(budgetStatus);
        
        // 에러 버짓 임계값 체크
        if (remainingBudgetPercent <= 10) { // 10% 미만 남았을 때
            sendErrorBudgetAlert(budgetStatus);
        }
    }

    // === 헬퍼 메서드들 ===

    private boolean checkServiceAvailability() {
        // 실제로는 Health Check 엔드포인트 호출
        // 여기서는 간단히 true 반환
        return true;
    }

    private LatencyStatistics getCurrentLatencyStatistics() {
        // 실제로는 메트릭 시스템에서 조회
        return LatencyStatistics.builder()
                .p50(120.0)
                .p95(450.0)
                .p99(800.0)
                .average(200.0)
                .build();
    }

    private ErrorRateStatistics getCurrentErrorRateStatistics() {
        // 실제로는 메트릭 시스템에서 조회
        return ErrorRateStatistics.builder()
                .totalRequests(1000L)
                .errorRequests(5L)
                .errorRate(0.5)
                .build();
    }

    private ThroughputStatistics getCurrentThroughputStatistics() {
        // 실제로는 메트릭 시스템에서 조회
        return ThroughputStatistics.builder()
                .requestsPerMinute(850.0)
                .requestsPerSecond(14.2)
                .build();
    }

    private BusinessMetrics getCurrentBusinessMetrics() {
        // 실제로는 메트릭 시스템에서 조회
        return BusinessMetrics.builder()
                .bookRegistrations(25L)
                .userLogins(150L)
                .bookBorrows(80L)
                .activeUsers(320L)
                .build();
    }

    // Redis 데이터 조회 메서드들
    private List<Integer> getAvailabilityDataFromRedis(LocalDateTime startTime, LocalDateTime endTime) {
        // 구현 생략 - 실제로는 Redis에서 시계열 데이터 조회
        return List.of(1, 1, 1, 0, 1, 1, 1); // 예시 데이터
    }

    private List<Map<String, Object>> getLatencyDataFromRedis(LocalDateTime startTime, LocalDateTime endTime) {
        // 구현 생략 - 실제로는 Redis에서 응답 시간 데이터 조회
        return List.of(
            Map.of("p95", 400.0),
            Map.of("p95", 520.0),
            Map.of("p95", 380.0)
        );
    }

    private List<Double> getErrorRateDataFromRedis(LocalDateTime startTime, LocalDateTime endTime) {
        // 구현 생략 - 실제로는 Redis에서 에러율 데이터 조회
        return List.of(0.1, 0.2, 0.05, 0.3);
    }

    private List<Double> getThroughputDataFromRedis(LocalDateTime startTime, LocalDateTime endTime) {
        // 구현 생략 - 실제로는 Redis에서 처리량 데이터 조회
        return List.of(800.0, 850.0, 900.0, 780.0);
    }

    // 기타 메서드들
    private SLOViolationSeverity determineSeverity(SLODefinition slo, SLOEvaluationResult result) {
        double deviation = slo.getTargetValue() - result.getCurrentValue();
        if (deviation > 5.0) {
            return SLOViolationSeverity.CRITICAL;
        } else if (deviation > 2.0) {
            return SLOViolationSeverity.HIGH;
        } else if (deviation > 1.0) {
            return SLOViolationSeverity.MEDIUM;
        } else {
            return SLOViolationSeverity.LOW;
        }
    }

    private void saveSLOEvaluationResult(SLODefinition slo, SLOEvaluationResult result, LocalDateTime evaluationTime) {
        // 구현 생략 - 실제로는 데이터베이스에 저장
    }

    private void saveSLOViolation(SLOViolation violation) {
        // 구현 생략 - 실제로는 데이터베이스에 저장
    }

    private void saveErrorBudgetStatus(ErrorBudgetStatus budgetStatus) {
        // 구현 생략 - 실제로는 Redis/데이터베이스에 저장
    }

    private void sendSLOViolationAlert(SLOViolation violation) {
        // 구현 생략 - 실제로는 알림 시스템으로 전송
        log.warn("SLO 위반 알림: {}", violation);
    }

    private void sendErrorBudgetAlert(ErrorBudgetStatus budgetStatus) {
        // 구현 생략 - 실제로는 알림 시스템으로 전송
        log.warn("에러 버짓 경고: {}", budgetStatus);
    }

    private void logSLOViolation(SLOViolation violation) {
        structuredLogger.logBusinessEvent(
            com.alibou.booknetwork.logging.BusinessEvent.builder()
                .eventName("slo_violation")
                .entityType("slo")
                .entityId(violation.getSloName())
                .action("evaluate")
                .status("violation")
                .metadata(Map.of(
                    "current_value", violation.getCurrentValue(),
                    "target_value", violation.getTargetValue(),
                    "severity", violation.getSeverity().name()
                ))
                .build()
        );
    }

    private SLOResult calculateDailySLOResult(SLODefinition slo, LocalDateTime reportDate) {
        // 구현 생략 - 일일 SLO 결과 계산
        return SLOResult.builder().build();
    }

    private Map<String, ErrorBudgetStatus> calculateErrorBudgetStatus(LocalDateTime reportDate) {
        // 구현 생략 - 에러 버짓 상태 계산
        return new HashMap<>();
    }

    private void saveSLAReport(SLAReport report) {
        // 구현 생략 - 보고서 저장
    }

    private void sendSLAReport(SLAReport report) {
        // 구현 생략 - 보고서 발송
    }

    private void updateDashboardData(LocalDateTime now) {
        // 구현 생략 - 대시보드 데이터 업데이트
    }
}