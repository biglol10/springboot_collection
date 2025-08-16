package com.alibou.booknetwork.chaos;

import com.alibou.booknetwork.logging.StructuredLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chaos Engineering 서비스
 * 
 * 이 클래스는 Netflix의 Chaos Monkey 원칙을 기반으로 시스템의 복원력을 테스트합니다.
 * 
 * 주요 기능:
 * 1. 무작위 서비스 장애 시뮬레이션
 * 2. 네트워크 지연 및 패킷 손실 주입
 * 3. 자원 고갈 시나리오 테스트
 * 4. 의존성 실패 시뮬레이션
 * 5. 데이터베이스 연결 장애 테스트
 * 
 * 왜 필요한가?
 * - 시스템 복원력 검증
 * - 장애 상황에서의 동작 확인
 * - 모니터링 시스템 검증
 * - 팀의 사고 대응 능력 향상
 * - 고가용성 아키텍처 검증
 * 
 * 주의사항:
 * - 운영 환경에서는 신중하게 활성화
 * - 비즈니스 시간 외에만 실행 권장
 * - 충분한 모니터링과 함께 사용
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "chaos.engineering.enabled", havingValue = "true", matchIfMissing = false)
public class ChaosEngineeringService {

    private final StructuredLogger structuredLogger;
    private final ChaosConfigurationProperties chaosConfig;
    private final Random random = new Random();
    
    // Chaos 실행 통계
    private int totalChaosExperiments = 0;
    private int successfulExperiments = 0;
    private int failedExperiments = 0;

    /**
     * 주기적으로 Chaos 실험을 실행합니다.
     * 매시간 정각에 실행되며, 설정에 따라 확률적으로 실험을 수행합니다.
     */
    @Scheduled(cron = "0 0 * * * *") // 매시간 정각
    public void executeScheduledChaosExperiments() {
        if (!shouldExecuteChaos()) {
            log.debug("Chaos 실험 조건이 맞지 않아 스킵됩니다.");
            return;
        }

        log.info("스케줄된 Chaos 실험을 시작합니다...");
        
        CompletableFuture.runAsync(() -> {
            try {
                ChaosExperiment experiment = selectRandomExperiment();
                executeExperiment(experiment);
            } catch (Exception e) {
                log.error("스케줄된 Chaos 실험 중 오류 발생", e);
                failedExperiments++;
            }
        });
    }

    /**
     * 수동으로 특정 Chaos 실험을 실행합니다.
     */
    public CompletableFuture<ChaosExperimentResult> executeExperiment(ChaosExperiment experiment) {
        return CompletableFuture.supplyAsync(() -> {
            totalChaosExperiments++;
            
            log.warn("Chaos 실험 시작: {} (강도: {})", 
                    experiment.getName(), experiment.getSeverity());
            
            ChaosExperimentResult result = ChaosExperimentResult.builder()
                    .experimentId(generateExperimentId())
                    .experimentName(experiment.getName())
                    .startTime(LocalDateTime.now())
                    .severity(experiment.getSeverity())
                    .build();

            try {
                // 실험 전 시스템 상태 캡처
                SystemHealthSnapshot preChaosSnapshot = captureSystemHealth();
                result.setPreChaosSnapshot(preChaosSnapshot);

                // Chaos 실험 실행
                switch (experiment.getType()) {
                    case LATENCY_INJECTION:
                        executeLatencyInjection(experiment);
                        break;
                    case MEMORY_PRESSURE:
                        executeMemoryPressure(experiment);
                        break;
                    case CPU_SPIKE:
                        executeCpuSpike(experiment);
                        break;
                    case NETWORK_PARTITION:
                        executeNetworkPartition(experiment);
                        break;
                    case DATABASE_FAILURE:
                        executeDatabaseFailure(experiment);
                        break;
                    case REDIS_FAILURE:
                        executeRedisFailure(experiment);
                        break;
                    case DISK_IO_STRESS:
                        executeDiskIoStress(experiment);
                        break;
                    case RANDOM_EXCEPTION:
                        executeRandomException(experiment);
                        break;
                    default:
                        throw new UnsupportedOperationException("지원하지 않는 실험 유형: " + experiment.getType());
                }

                // 실험 중 시스템 관찰
                Thread.sleep(experiment.getDurationSeconds() * 1000);

                // 실험 후 시스템 상태 캡처
                SystemHealthSnapshot postChaosSnapshot = captureSystemHealth();
                result.setPostChaosSnapshot(postChaosSnapshot);

                // 복구 작업 수행
                performRecovery(experiment);

                result.setEndTime(LocalDateTime.now());
                result.setStatus(ChaosExperimentStatus.COMPLETED);
                result.setImpactAssessment(assessImpact(preChaosSnapshot, postChaosSnapshot));

                successfulExperiments++;
                log.info("Chaos 실험 완료: {} (결과: {})", 
                        experiment.getName(), result.getStatus());

            } catch (Exception e) {
                result.setEndTime(LocalDateTime.now());
                result.setStatus(ChaosExperimentStatus.FAILED);
                result.setErrorMessage(e.getMessage());
                
                failedExperiments++;
                log.error("Chaos 실험 실패: {}", experiment.getName(), e);
                
                // 실패 시 즉시 복구 시도
                try {
                    performRecovery(experiment);
                } catch (Exception recoveryException) {
                    log.error("Chaos 실험 복구 실패: {}", experiment.getName(), recoveryException);
                }
            }

            // 결과 로깅
            logExperimentResult(result);
            
            return result;
        });
    }

    /**
     * 지연 주입 실험
     */
    private void executeLatencyInjection(ChaosExperiment experiment) throws InterruptedException {
        log.warn("지연 주입 실험 시작 - {}ms 지연 추가", experiment.getIntensity());
        
        // 전역 지연 플래그 설정
        ChaosContext.enableLatencyInjection(experiment.getIntensity());
        
        // 실험 실행 시간만큼 대기
        Thread.sleep(experiment.getDurationSeconds() * 1000);
        
        // 지연 플래그 해제
        ChaosContext.disableLatencyInjection();
        
        log.info("지연 주입 실험 종료");
    }

    /**
     * 메모리 압박 실험
     */
    private void executeMemoryPressure(ChaosExperiment experiment) throws InterruptedException {
        log.warn("메모리 압박 실험 시작 - {}MB 메모리 할당", experiment.getIntensity());
        
        List<byte[]> memoryHogs = new java.util.ArrayList<>();
        
        try {
            // 메모리 할당
            int memoryToAllocateMB = experiment.getIntensity();
            for (int i = 0; i < memoryToAllocateMB; i++) {
                memoryHogs.add(new byte[1024 * 1024]); // 1MB씩 할당
                Thread.sleep(100); // 점진적 할당
            }
            
            // 실험 실행 시간만큼 대기
            Thread.sleep(experiment.getDurationSeconds() * 1000);
            
        } finally {
            // 메모리 해제
            memoryHogs.clear();
            System.gc();
            log.info("메모리 압박 실험 종료 - 메모리 해제됨");
        }
    }

    /**
     * CPU 스파이크 실험
     */
    private void executeCpuSpike(ChaosExperiment experiment) {
        log.warn("CPU 스파이크 실험 시작 - {}% CPU 사용률 목표", experiment.getIntensity());
        
        int threads = experiment.getIntensity() / 10; // 강도에 따른 스레드 수
        List<CompletableFuture<Void>> cpuTasks = new java.util.ArrayList<>();
        
        for (int i = 0; i < threads; i++) {
            cpuTasks.add(CompletableFuture.runAsync(() -> {
                long endTime = System.currentTimeMillis() + (experiment.getDurationSeconds() * 1000);
                while (System.currentTimeMillis() < endTime) {
                    // CPU 집약적 작업
                    Math.sqrt(ThreadLocalRandom.current().nextDouble());
                }
            }));
        }
        
        // 모든 CPU 작업 완료 대기
        CompletableFuture.allOf(cpuTasks.toArray(new CompletableFuture[0])).join();
        
        log.info("CPU 스파이크 실험 종료");
    }

    /**
     * 네트워크 파티션 실험
     */
    private void executeNetworkPartition(ChaosExperiment experiment) throws InterruptedException {
        log.warn("네트워크 파티션 실험 시작 - {}% 패킷 드롭", experiment.getIntensity());
        
        // 네트워크 장애 시뮬레이션 플래그 설정
        ChaosContext.enableNetworkPartition(experiment.getIntensity());
        
        // 실험 실행 시간만큼 대기
        Thread.sleep(experiment.getDurationSeconds() * 1000);
        
        // 네트워크 장애 플래그 해제
        ChaosContext.disableNetworkPartition();
        
        log.info("네트워크 파티션 실험 종료");
    }

    /**
     * 데이터베이스 장애 실험
     */
    private void executeDatabaseFailure(ChaosExperiment experiment) throws InterruptedException {
        log.warn("데이터베이스 장애 실험 시작 - {}% 실패율", experiment.getIntensity());
        
        // 데이터베이스 장애 시뮬레이션 플래그 설정
        ChaosContext.enableDatabaseFailure(experiment.getIntensity());
        
        // 실험 실행 시간만큼 대기
        Thread.sleep(experiment.getDurationSeconds() * 1000);
        
        // 데이터베이스 장애 플래그 해제
        ChaosContext.disableDatabaseFailure();
        
        log.info("데이터베이스 장애 실험 종료");
    }

    /**
     * Redis 장애 실험
     */
    private void executeRedisFailure(ChaosExperiment experiment) throws InterruptedException {
        log.warn("Redis 장애 실험 시작 - {}% 실패율", experiment.getIntensity());
        
        // Redis 장애 시뮬레이션 플래그 설정
        ChaosContext.enableRedisFailure(experiment.getIntensity());
        
        // 실험 실행 시간만큼 대기
        Thread.sleep(experiment.getDurationSeconds() * 1000);
        
        // Redis 장애 플래그 해제
        ChaosContext.disableRedisFailure();
        
        log.info("Redis 장애 실험 종료");
    }

    /**
     * 디스크 I/O 스트레스 실험
     */
    private void executeDiskIoStress(ChaosExperiment experiment) throws InterruptedException {
        log.warn("디스크 I/O 스트레스 실험 시작");
        
        CompletableFuture<Void> ioTask = CompletableFuture.runAsync(() -> {
            try {
                java.io.File tempFile = java.io.File.createTempFile("chaos_io_test", ".tmp");
                tempFile.deleteOnExit();
                
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                    byte[] data = new byte[1024];
                    long endTime = System.currentTimeMillis() + (experiment.getDurationSeconds() * 1000);
                    
                    while (System.currentTimeMillis() < endTime) {
                        fos.write(data);
                        fos.flush();
                        Thread.sleep(10);
                    }
                }
                
                tempFile.delete();
                
            } catch (Exception e) {
                log.error("디스크 I/O 스트레스 실험 중 오류", e);
            }
        });
        
        ioTask.join();
        log.info("디스크 I/O 스트레스 실험 종료");
    }

    /**
     * 무작위 예외 발생 실험
     */
    private void executeRandomException(ChaosExperiment experiment) throws InterruptedException {
        log.warn("무작위 예외 발생 실험 시작 - {}% 예외 발생률", experiment.getIntensity());
        
        // 무작위 예외 플래그 설정
        ChaosContext.enableRandomExceptions(experiment.getIntensity());
        
        // 실험 실행 시간만큼 대기
        Thread.sleep(experiment.getDurationSeconds() * 1000);
        
        // 무작위 예외 플래그 해제
        ChaosContext.disableRandomExceptions();
        
        log.info("무작위 예외 발생 실험 종료");
    }

    /**
     * 실험 실행 조건 확인
     */
    private boolean shouldExecuteChaos() {
        // 1. 환경 확인 (운영 환경에서는 추가 제약)
        if (chaosConfig.isProductionEnvironment() && !chaosConfig.isProductionChaosEnabled()) {
            return false;
        }

        // 2. 시간대 확인 (비즈니스 시간 외에만 실행)
        if (!isValidTimeWindow()) {
            return false;
        }

        // 3. 확률 기반 실행
        return random.nextDouble() < chaosConfig.getExecutionProbability();
    }

    /**
     * 유효한 시간 윈도우 확인
     */
    private boolean isValidTimeWindow() {
        LocalTime now = LocalTime.now();
        
        // 평일 업무시간 (9-18시) 제외
        if (isWeekday() && now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(18, 0))) {
            return false;
        }
        
        // 설정된 금지 시간대 확인
        return chaosConfig.getAllowedTimeWindows().stream()
                .anyMatch(window -> window.contains(now));
    }

    /**
     * 평일 여부 확인
     */
    private boolean isWeekday() {
        java.time.DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();
        return today.getValue() >= 1 && today.getValue() <= 5;
    }

    /**
     * 무작위 실험 선택
     */
    private ChaosExperiment selectRandomExperiment() {
        List<ChaosExperiment> availableExperiments = chaosConfig.getAvailableExperiments();
        return availableExperiments.get(random.nextInt(availableExperiments.size()));
    }

    /**
     * 실험 ID 생성
     */
    private String generateExperimentId() {
        return "chaos-" + System.currentTimeMillis() + "-" + random.nextInt(1000);
    }

    /**
     * 시스템 상태 캡처
     */
    private SystemHealthSnapshot captureSystemHealth() {
        Runtime runtime = Runtime.getRuntime();
        
        return SystemHealthSnapshot.builder()
                .timestamp(LocalDateTime.now())
                .totalMemoryMB(runtime.totalMemory() / (1024 * 1024))
                .freeMemoryMB(runtime.freeMemory() / (1024 * 1024))
                .maxMemoryMB(runtime.maxMemory() / (1024 * 1024))
                .activeThreads(Thread.activeCount())
                // 실제 환경에서는 CPU, 네트워크, 디스크 메트릭도 수집
                .build();
    }

    /**
     * 영향 평가
     */
    private String assessImpact(SystemHealthSnapshot before, SystemHealthSnapshot after) {
        StringBuilder assessment = new StringBuilder();
        
        long memoryDiff = before.getFreeMemoryMB() - after.getFreeMemoryMB();
        if (memoryDiff > 100) {
            assessment.append("High memory consumption detected (+").append(memoryDiff).append("MB). ");
        }
        
        int threadDiff = after.getActiveThreads() - before.getActiveThreads();
        if (threadDiff > 10) {
            assessment.append("Thread count increased (+").append(threadDiff).append("). ");
        }
        
        if (assessment.length() == 0) {
            assessment.append("Minimal system impact detected.");
        }
        
        return assessment.toString();
    }

    /**
     * 복구 작업 수행
     */
    private void performRecovery(ChaosExperiment experiment) {
        log.info("Chaos 실험 복구 작업 시작: {}", experiment.getName());
        
        // 모든 Chaos 컨텍스트 정리
        ChaosContext.disableAll();
        
        // 가비지 컬렉션 실행
        System.gc();
        
        // 추가 복구 작업이 필요한 경우 여기에 구현
        
        log.info("Chaos 실험 복구 작업 완료: {}", experiment.getName());
    }

    /**
     * 실험 결과 로깅
     */
    private void logExperimentResult(ChaosExperimentResult result) {
        structuredLogger.logBusinessEvent(
            com.alibou.booknetwork.logging.BusinessEvent.builder()
                .eventName("chaos_experiment_completed")
                .entityType("chaos_experiment")
                .entityId(result.getExperimentId())
                .action("execute")
                .status(result.getStatus().name().toLowerCase())
                .duration(result.getDurationMs())
                .metadata(java.util.Map.of(
                    "experiment_name", result.getExperimentName(),
                    "severity", result.getSeverity().name(),
                    "impact_assessment", result.getImpactAssessment()
                ))
                .build()
        );
    }

    /**
     * Chaos 실험 통계 반환
     */
    public ChaosStatistics getStatistics() {
        return ChaosStatistics.builder()
                .totalExperiments(totalChaosExperiments)
                .successfulExperiments(successfulExperiments)
                .failedExperiments(failedExperiments)
                .successRate(totalChaosExperiments > 0 ? 
                    (double) successfulExperiments / totalChaosExperiments : 0.0)
                .build();
    }

    /**
     * 모든 Chaos 활동 즉시 중단
     */
    public void emergencyStop() {
        log.warn("Chaos Engineering 비상 중단 요청");
        ChaosContext.disableAll();
        log.info("모든 Chaos 활동이 중단되었습니다");
    }
}