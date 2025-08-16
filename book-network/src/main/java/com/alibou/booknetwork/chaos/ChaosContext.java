package com.alibou.booknetwork.chaos;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chaos Engineering 컨텍스트
 * 
 * 이 클래스는 애플리케이션 전반에서 Chaos 실험의 상태를 관리하고
 * 실제 비즈니스 로직에 장애를 주입하는 역할을 합니다.
 * 
 * 주요 기능:
 * 1. 전역 Chaos 플래그 관리
 * 2. 각 실험별 강도 조절
 * 3. 실시간 장애 주입
 * 4. 스레드 안전성 보장
 * 
 * 사용법:
 * - ChaosContext.isLatencyInjectionEnabled() - 지연 주입 여부 확인
 * - ChaosContext.shouldFailDatabaseOperation() - DB 작업 실패 여부 결정
 * - ChaosContext.getLatencyDelayMs() - 추가할 지연 시간 반환
 */
@Slf4j
public final class ChaosContext {

    // 지연 주입 관련
    private static final AtomicBoolean latencyInjectionEnabled = new AtomicBoolean(false);
    private static final AtomicInteger latencyDelayMs = new AtomicInteger(0);

    // 메모리 압박 관련
    private static final AtomicBoolean memoryPressureEnabled = new AtomicBoolean(false);
    private static final AtomicInteger memoryPressureIntensity = new AtomicInteger(0);

    // 네트워크 파티션 관련
    private static final AtomicBoolean networkPartitionEnabled = new AtomicBoolean(false);
    private static final AtomicInteger networkFailureRate = new AtomicInteger(0);

    // 데이터베이스 장애 관련
    private static final AtomicBoolean databaseFailureEnabled = new AtomicBoolean(false);
    private static final AtomicInteger databaseFailureRate = new AtomicInteger(0);

    // Redis 장애 관련
    private static final AtomicBoolean redisFailureEnabled = new AtomicBoolean(false);
    private static final AtomicInteger redisFailureRate = new AtomicInteger(0);

    // 무작위 예외 관련
    private static final AtomicBoolean randomExceptionsEnabled = new AtomicBoolean(false);
    private static final AtomicInteger randomExceptionRate = new AtomicInteger(0);

    // 전역 Chaos 제어
    private static final AtomicBoolean globalChaosEnabled = new AtomicBoolean(true);

    private ChaosContext() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    // === 지연 주입 관련 메서드 ===

    /**
     * 지연 주입을 활성화합니다.
     */
    public static void enableLatencyInjection(int delayMs) {
        latencyDelayMs.set(delayMs);
        latencyInjectionEnabled.set(true);
        log.debug("지연 주입 활성화: {}ms", delayMs);
    }

    /**
     * 지연 주입을 비활성화합니다.
     */
    public static void disableLatencyInjection() {
        latencyInjectionEnabled.set(false);
        latencyDelayMs.set(0);
        log.debug("지연 주입 비활성화");
    }

    /**
     * 지연 주입이 활성화되어 있는지 확인합니다.
     */
    public static boolean isLatencyInjectionEnabled() {
        return globalChaosEnabled.get() && latencyInjectionEnabled.get();
    }

    /**
     * 현재 설정된 지연 시간을 반환합니다.
     */
    public static int getLatencyDelayMs() {
        return isLatencyInjectionEnabled() ? latencyDelayMs.get() : 0;
    }

    /**
     * 지연을 주입합니다. 비즈니스 로직에서 호출하여 사용합니다.
     */
    public static void injectLatency() {
        if (isLatencyInjectionEnabled()) {
            int delay = getLatencyDelayMs();
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                    log.trace("지연 주입됨: {}ms", delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("지연 주입 중 인터럽트 발생", e);
                }
            }
        }
    }

    // === 네트워크 파티션 관련 메서드 ===

    /**
     * 네트워크 파티션을 활성화합니다.
     */
    public static void enableNetworkPartition(int failureRatePercent) {
        networkFailureRate.set(Math.min(100, Math.max(0, failureRatePercent)));
        networkPartitionEnabled.set(true);
        log.debug("네트워크 파티션 활성화: {}% 실패율", failureRatePercent);
    }

    /**
     * 네트워크 파티션을 비활성화합니다.
     */
    public static void disableNetworkPartition() {
        networkPartitionEnabled.set(false);
        networkFailureRate.set(0);
        log.debug("네트워크 파티션 비활성화");
    }

    /**
     * 네트워크 작업이 실패해야 하는지 결정합니다.
     */
    public static boolean shouldFailNetworkOperation() {
        if (!globalChaosEnabled.get() || !networkPartitionEnabled.get()) {
            return false;
        }

        int failureRate = networkFailureRate.get();
        boolean shouldFail = ThreadLocalRandom.current().nextInt(100) < failureRate;
        
        if (shouldFail) {
            log.trace("네트워크 작업 실패 주입됨");
        }
        
        return shouldFail;
    }

    // === 데이터베이스 장애 관련 메서드 ===

    /**
     * 데이터베이스 장애를 활성화합니다.
     */
    public static void enableDatabaseFailure(int failureRatePercent) {
        databaseFailureRate.set(Math.min(100, Math.max(0, failureRatePercent)));
        databaseFailureEnabled.set(true);
        log.debug("데이터베이스 장애 활성화: {}% 실패율", failureRatePercent);
    }

    /**
     * 데이터베이스 장애를 비활성화합니다.
     */
    public static void disableDatabaseFailure() {
        databaseFailureEnabled.set(false);
        databaseFailureRate.set(0);
        log.debug("데이터베이스 장애 비활성화");
    }

    /**
     * 데이터베이스 작업이 실패해야 하는지 결정합니다.
     */
    public static boolean shouldFailDatabaseOperation() {
        if (!globalChaosEnabled.get() || !databaseFailureEnabled.get()) {
            return false;
        }

        int failureRate = databaseFailureRate.get();
        boolean shouldFail = ThreadLocalRandom.current().nextInt(100) < failureRate;
        
        if (shouldFail) {
            log.trace("데이터베이스 작업 실패 주입됨");
        }
        
        return shouldFail;
    }

    // === Redis 장애 관련 메서드 ===

    /**
     * Redis 장애를 활성화합니다.
     */
    public static void enableRedisFailure(int failureRatePercent) {
        redisFailureRate.set(Math.min(100, Math.max(0, failureRatePercent)));
        redisFailureEnabled.set(true);
        log.debug("Redis 장애 활성화: {}% 실패율", failureRatePercent);
    }

    /**
     * Redis 장애를 비활성화합니다.
     */
    public static void disableRedisFailure() {
        redisFailureEnabled.set(false);
        redisFailureRate.set(0);
        log.debug("Redis 장애 비활성화");
    }

    /**
     * Redis 작업이 실패해야 하는지 결정합니다.
     */
    public static boolean shouldFailRedisOperation() {
        if (!globalChaosEnabled.get() || !redisFailureEnabled.get()) {
            return false;
        }

        int failureRate = redisFailureRate.get();
        boolean shouldFail = ThreadLocalRandom.current().nextInt(100) < failureRate;
        
        if (shouldFail) {
            log.trace("Redis 작업 실패 주입됨");
        }
        
        return shouldFail;
    }

    // === 무작위 예외 관련 메서드 ===

    /**
     * 무작위 예외 발생을 활성화합니다.
     */
    public static void enableRandomExceptions(int exceptionRatePercent) {
        randomExceptionRate.set(Math.min(100, Math.max(0, exceptionRatePercent)));
        randomExceptionsEnabled.set(true);
        log.debug("무작위 예외 활성화: {}% 발생률", exceptionRatePercent);
    }

    /**
     * 무작위 예외 발생을 비활성화합니다.
     */
    public static void disableRandomExceptions() {
        randomExceptionsEnabled.set(false);
        randomExceptionRate.set(0);
        log.debug("무작위 예외 비활성화");
    }

    /**
     * 무작위 예외를 발생시켜야 하는지 결정합니다.
     */
    public static boolean shouldThrowRandomException() {
        if (!globalChaosEnabled.get() || !randomExceptionsEnabled.get()) {
            return false;
        }

        int exceptionRate = randomExceptionRate.get();
        boolean shouldThrow = ThreadLocalRandom.current().nextInt(100) < exceptionRate;
        
        if (shouldThrow) {
            log.trace("무작위 예외 발생 주입됨");
        }
        
        return shouldThrow;
    }

    /**
     * 무작위 예외를 생성합니다.
     */
    public static RuntimeException createRandomException() {
        String[] exceptionMessages = {
            "Chaos Monkey: 시뮬레이션된 서비스 장애",
            "Chaos Monkey: 임시적인 시스템 오류",
            "Chaos Monkey: 의존성 서비스 불가용",
            "Chaos Monkey: 타임아웃 시뮬레이션",
            "Chaos Monkey: 자원 부족 시뮬레이션"
        };

        RuntimeException[] exceptionTypes = {
            new ChaosException("Chaos Monkey 실험 중"),
            new RuntimeException("Chaos: 일시적 시스템 오류"),
            new IllegalStateException("Chaos: 잘못된 시스템 상태"),
            new java.util.concurrent.TimeoutException("Chaos: 작업 타임아웃").getCause() != null ? 
                new RuntimeException("Chaos: 작업 타임아웃") : new RuntimeException("Chaos: 작업 타임아웃")
        };

        int index = ThreadLocalRandom.current().nextInt(exceptionTypes.length);
        return exceptionTypes[index];
    }

    // === 전역 제어 메서드 ===

    /**
     * 전역 Chaos를 비활성화합니다.
     */
    public static void disableGlobalChaos() {
        globalChaosEnabled.set(false);
        log.warn("전역 Chaos 비활성화됨");
    }

    /**
     * 전역 Chaos를 활성화합니다.
     */
    public static void enableGlobalChaos() {
        globalChaosEnabled.set(true);
        log.info("전역 Chaos 활성화됨");
    }

    /**
     * 전역 Chaos 활성화 상태를 확인합니다.
     */
    public static boolean isGlobalChaosEnabled() {
        return globalChaosEnabled.get();
    }

    /**
     * 모든 Chaos 활동을 즉시 중단합니다.
     */
    public static void disableAll() {
        disableLatencyInjection();
        disableNetworkPartition();
        disableDatabaseFailure();
        disableRedisFailure();
        disableRandomExceptions();
        log.warn("모든 Chaos 활동이 비활성화되었습니다");
    }

    // === 유틸리티 메서드 ===

    /**
     * 현재 Chaos 상태를 반환합니다.
     */
    public static ChaosStatus getCurrentStatus() {
        return ChaosStatus.builder()
                .globalEnabled(globalChaosEnabled.get())
                .latencyInjectionEnabled(latencyInjectionEnabled.get())
                .latencyDelayMs(latencyDelayMs.get())
                .networkPartitionEnabled(networkPartitionEnabled.get())
                .networkFailureRate(networkFailureRate.get())
                .databaseFailureEnabled(databaseFailureEnabled.get())
                .databaseFailureRate(databaseFailureRate.get())
                .redisFailureEnabled(redisFailureEnabled.get())
                .redisFailureRate(redisFailureRate.get())
                .randomExceptionsEnabled(randomExceptionsEnabled.get())
                .randomExceptionRate(randomExceptionRate.get())
                .build();
    }

    /**
     * Chaos 컨텍스트가 활성화되어 있는지 확인합니다.
     */
    public static boolean isAnyChaosActive() {
        return globalChaosEnabled.get() && (
            latencyInjectionEnabled.get() ||
            networkPartitionEnabled.get() ||
            databaseFailureEnabled.get() ||
            redisFailureEnabled.get() ||
            randomExceptionsEnabled.get()
        );
    }

    /**
     * 비즈니스 로직에서 호출하여 종합적인 Chaos 주입을 수행합니다.
     */
    public static void injectChaos(String operationName) {
        if (!globalChaosEnabled.get()) {
            return;
        }

        // 지연 주입
        injectLatency();

        // 무작위 예외 주입
        if (shouldThrowRandomException()) {
            log.debug("Chaos 예외 주입: {}", operationName);
            throw createRandomException();
        }
    }

    /**
     * 특정 서비스 작업에 대한 Chaos 주입
     */
    public static void injectServiceChaos(String serviceName, String operation) {
        if (!globalChaosEnabled.get()) {
            return;
        }

        log.trace("Chaos 검사: {}.{}", serviceName, operation);

        // 서비스별 지연 주입
        injectLatency();

        // 서비스별 실패 주입
        switch (serviceName.toLowerCase()) {
            case "database":
            case "db":
                if (shouldFailDatabaseOperation()) {
                    throw new ChaosException("Chaos: 데이터베이스 작업 실패 시뮬레이션");
                }
                break;
            case "redis":
            case "cache":
                if (shouldFailRedisOperation()) {
                    throw new ChaosException("Chaos: Redis 작업 실패 시뮬레이션");
                }
                break;
            case "network":
            case "http":
                if (shouldFailNetworkOperation()) {
                    throw new ChaosException("Chaos: 네트워크 작업 실패 시뮬레이션");
                }
                break;
            default:
                // 일반적인 무작위 예외
                if (shouldThrowRandomException()) {
                    throw createRandomException();
                }
        }
    }

    // === 커스텀 예외 클래스 ===

    /**
     * Chaos Engineering 전용 예외 클래스
     */
    public static class ChaosException extends RuntimeException {
        public ChaosException(String message) {
            super(message);
        }

        public ChaosException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}