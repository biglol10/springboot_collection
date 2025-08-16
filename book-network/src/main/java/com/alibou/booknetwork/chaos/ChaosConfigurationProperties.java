package com.alibou.booknetwork.chaos;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Chaos Engineering 설정 프로퍼티
 * 
 * application.yml에서 chaos.engineering으로 시작하는 모든 설정을 바인딩합니다.
 * 
 * 설정 예시:
 * chaos:
 *   engineering:
 *     enabled: true
 *     production-chaos-enabled: false
 *     execution-probability: 0.1
 *     allowed-time-windows:
 *       - start: "22:00"
 *         end: "06:00"
 */
@Data
@Component
@ConfigurationProperties(prefix = "chaos.engineering")
public class ChaosConfigurationProperties {

    /**
     * Chaos Engineering 전체 활성화 여부
     */
    private boolean enabled = false;

    /**
     * 운영 환경에서의 Chaos 실행 허용 여부
     */
    private boolean productionChaosEnabled = false;

    /**
     * 현재 환경이 운영 환경인지 여부
     */
    private boolean productionEnvironment = false;

    /**
     * Chaos 실험 실행 확률 (0.0 ~ 1.0)
     */
    private double executionProbability = 0.05;

    /**
     * 허용된 시간 윈도우 목록
     */
    private List<TimeWindow> allowedTimeWindows = new ArrayList<>();

    /**
     * 사용 가능한 실험 목록
     */
    private List<ChaosExperiment> availableExperiments = new ArrayList<>();

    /**
     * 알림 설정
     */
    private NotificationConfig notifications = new NotificationConfig();

    /**
     * 안전 장치 설정
     */
    private SafetyConfig safety = new SafetyConfig();

    /**
     * 기본 설정 초기화
     */
    public ChaosConfigurationProperties() {
        initializeDefaultTimeWindows();
        initializeDefaultExperiments();
    }

    /**
     * 기본 시간 윈도우 초기화 (밤 시간대)
     */
    private void initializeDefaultTimeWindows() {
        // 평일 야간 시간 (22:00 ~ 06:00)
        allowedTimeWindows.add(new TimeWindow(LocalTime.of(22, 0), LocalTime.of(23, 59)));
        allowedTimeWindows.add(new TimeWindow(LocalTime.of(0, 0), LocalTime.of(6, 0)));

        // 주말 전체 시간 (토요일, 일요일)
        // 실제로는 요일별 설정이 필요하지만 여기서는 시간대만 설정
        allowedTimeWindows.add(new TimeWindow(LocalTime.of(6, 0), LocalTime.of(22, 0)));
    }

    /**
     * 기본 실험 목록 초기화
     */
    private void initializeDefaultExperiments() {
        // 지연 주입 실험
        availableExperiments.add(
            ChaosExperiment.builder()
                .name("Latency Injection")
                .type(ChaosExperimentType.LATENCY_INJECTION)
                .severity(ChaosSeverity.LOW)
                .intensity(500) // 500ms 지연
                .durationSeconds(300) // 5분
                .description("네트워크 지연 시뮬레이션")
                .build()
        );

        // 메모리 압박 실험
        availableExperiments.add(
            ChaosExperiment.builder()
                .name("Memory Pressure")
                .type(ChaosExperimentType.MEMORY_PRESSURE)
                .severity(ChaosSeverity.MEDIUM)
                .intensity(256) // 256MB 할당
                .durationSeconds(180) // 3분
                .description("메모리 부족 상황 시뮬레이션")
                .build()
        );

        // CPU 스파이크 실험
        availableExperiments.add(
            ChaosExperiment.builder()
                .name("CPU Spike")
                .type(ChaosExperimentType.CPU_SPIKE)
                .severity(ChaosSeverity.MEDIUM)
                .intensity(80) // 80% CPU 사용률
                .durationSeconds(120) // 2분
                .description("CPU 부하 시뮬레이션")
                .build()
        );

        // 데이터베이스 장애 실험
        availableExperiments.add(
            ChaosExperiment.builder()
                .name("Database Failure")
                .type(ChaosExperimentType.DATABASE_FAILURE)
                .severity(ChaosSeverity.HIGH)
                .intensity(20) // 20% 실패율
                .durationSeconds(60) // 1분
                .description("데이터베이스 연결 장애 시뮬레이션")
                .build()
        );

        // Redis 장애 실험
        availableExperiments.add(
            ChaosExperiment.builder()
                .name("Redis Failure")
                .type(ChaosExperimentType.REDIS_FAILURE)
                .severity(ChaosSeverity.MEDIUM)
                .intensity(30) // 30% 실패율
                .durationSeconds(90) // 1.5분
                .description("Redis 캐시 장애 시뮬레이션")
                .build()
        );

        // 네트워크 파티션 실험
        availableExperiments.add(
            ChaosExperiment.builder()
                .name("Network Partition")
                .type(ChaosExperimentType.NETWORK_PARTITION)
                .severity(ChaosSeverity.HIGH)
                .intensity(15) // 15% 패킷 드롭
                .durationSeconds(180) // 3분
                .description("네트워크 분할 시뮬레이션")
                .build()
        );

        // 무작위 예외 실험
        availableExperiments.add(
            ChaosExperiment.builder()
                .name("Random Exceptions")
                .type(ChaosExperimentType.RANDOM_EXCEPTION)
                .severity(ChaosSeverity.LOW)
                .intensity(5) // 5% 예외 발생률
                .durationSeconds(300) // 5분
                .description("무작위 예외 발생 시뮬레이션")
                .build()
        );
    }

    /**
     * 시간 윈도우 클래스
     */
    @Data
    public static class TimeWindow {
        private LocalTime start;
        private LocalTime end;

        public TimeWindow() {}

        public TimeWindow(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        /**
         * 주어진 시간이 이 윈도우에 포함되는지 확인
         */
        public boolean contains(LocalTime time) {
            if (start.isBefore(end)) {
                // 일반적인 경우 (예: 09:00 ~ 17:00)
                return !time.isBefore(start) && !time.isAfter(end);
            } else {
                // 자정을 넘어가는 경우 (예: 22:00 ~ 06:00)
                return !time.isBefore(start) || !time.isAfter(end);
            }
        }
    }

    /**
     * 알림 설정 클래스
     */
    @Data
    public static class NotificationConfig {
        /**
         * 실험 시작 시 알림 발송 여부
         */
        private boolean notifyOnStart = true;

        /**
         * 실험 완료 시 알림 발송 여부
         */
        private boolean notifyOnComplete = true;

        /**
         * 실험 실패 시 알림 발송 여부
         */
        private boolean notifyOnFailure = true;

        /**
         * Slack 웹훅 URL
         */
        private String slackWebhookUrl;

        /**
         * 이메일 알림 수신자 목록
         */
        private List<String> emailRecipients = new ArrayList<>();

        /**
         * 알림 채널 설정
         */
        private String defaultChannel = "chaos-engineering";
        private String criticalChannel = "chaos-critical";
    }

    /**
     * 안전 장치 설정 클래스
     */
    @Data
    public static class SafetyConfig {
        /**
         * 동시 실행 가능한 최대 실험 수
         */
        private int maxConcurrentExperiments = 1;

        /**
         * 실험 간 최소 간격 (분)
         */
        private int minIntervalMinutes = 30;

        /**
         * 하루 최대 실험 수
         */
        private int maxExperimentsPerDay = 10;

        /**
         * 비상 중단 키워드 (로그에서 감지 시 모든 실험 중단)
         */
        private List<String> emergencyStopKeywords = List.of(
            "CRITICAL_ERROR",
            "SYSTEM_FAILURE",
            "EMERGENCY_STOP"
        );

        /**
         * 시스템 부하 임계값 (CPU 사용률 %)
         */
        private double cpuThreshold = 90.0;

        /**
         * 메모리 사용률 임계값 (%)
         */
        private double memoryThreshold = 90.0;

        /**
         * 에러율 임계값 (%)
         */
        private double errorRateThreshold = 10.0;
    }

    // === 편의 메서드들 ===

    /**
     * 심각도별 실험 목록 반환
     */
    public List<ChaosExperiment> getExperimentsBySeverity(ChaosSeverity severity) {
        return availableExperiments.stream()
                .filter(experiment -> experiment.getSeverity() == severity)
                .toList();
    }

    /**
     * 실험 타입별 실험 목록 반환
     */
    public List<ChaosExperiment> getExperimentsByType(ChaosExperimentType type) {
        return availableExperiments.stream()
                .filter(experiment -> experiment.getType() == type)
                .toList();
    }

    /**
     * 운영 환경에서 안전한 실험 목록 반환
     */
    public List<ChaosExperiment> getSafeExperimentsForProduction() {
        return availableExperiments.stream()
                .filter(experiment -> experiment.getSeverity() == ChaosSeverity.LOW)
                .toList();
    }

    /**
     * 현재 시간이 허용된 시간 윈도우 내인지 확인
     */
    public boolean isCurrentTimeAllowed() {
        LocalTime now = LocalTime.now();
        return allowedTimeWindows.stream()
                .anyMatch(window -> window.contains(now));
    }

    /**
     * 설정 유효성 검증
     */
    public void validate() {
        if (executionProbability < 0.0 || executionProbability > 1.0) {
            throw new IllegalArgumentException("실행 확률은 0.0과 1.0 사이여야 합니다: " + executionProbability);
        }

        if (safety.maxConcurrentExperiments < 1) {
            throw new IllegalArgumentException("최대 동시 실험 수는 1 이상이어야 합니다: " + safety.maxConcurrentExperiments);
        }

        if (safety.minIntervalMinutes < 1) {
            throw new IllegalArgumentException("최소 간격은 1분 이상이어야 합니다: " + safety.minIntervalMinutes);
        }

        if (allowedTimeWindows.isEmpty()) {
            throw new IllegalArgumentException("허용된 시간 윈도우가 최소 하나는 있어야 합니다");
        }

        if (availableExperiments.isEmpty()) {
            throw new IllegalArgumentException("사용 가능한 실험이 최소 하나는 있어야 합니다");
        }
    }
}