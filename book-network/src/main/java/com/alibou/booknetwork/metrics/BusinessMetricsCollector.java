package com.alibou.booknetwork.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 비즈니스 메트릭 수집기
 * 
 * 이 클래스는 애플리케이션의 핵심 비즈니스 메트릭을 수집하고 Prometheus로 노출합니다.
 * 
 * 주요 기능:
 * 1. 도서 관련 메트릭 (등록, 조회, 대여 등)
 * 2. 사용자 활동 메트릭 (로그인, 회원가입 등)
 * 3. 시스템 성능 메트릭 (응답 시간, 처리량 등)
 * 4. 캐시 효율성 메트릭 (히트율, 미스율 등)
 * 
 * 왜 필요한가?
 * - 비즈니스 KPI 실시간 모니터링
 * - 성능 병목 지점 식별
 * - 사용자 행동 패턴 분석
 * - 장애 예방 및 빠른 대응
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, Object> redisTemplate;

    // 비즈니스 메트릭 카운터들
    private Counter bookRegistrationCounter;
    private Counter bookBorrowCounter;
    private Counter bookReturnCounter;
    private Counter userLoginCounter;
    private Counter userRegistrationCounter;
    private Counter feedbackSubmissionCounter;

    // 성능 메트릭 타이머들
    private Timer bookSearchTimer;
    private Timer bookRecommendationTimer;
    private Timer emailSendingTimer;
    private Timer databaseQueryTimer;

    // 시스템 상태 게이지들
    private final AtomicLong activeUsersCount = new AtomicLong(0);
    private final AtomicLong totalBooksCount = new AtomicLong(0);
    private final AtomicLong borrowedBooksCount = new AtomicLong(0);

    @PostConstruct
    public void initializeMetrics() {
        log.info("비즈니스 메트릭 수집기를 초기화합니다...");

        // 비즈니스 활동 카운터 초기화
        bookRegistrationCounter = Counter.builder("books.registered.total")
                .description("총 등록된 도서 수")
                .tag("operation", "registration")
                .register(meterRegistry);

        bookBorrowCounter = Counter.builder("books.borrowed.total")
                .description("총 대여된 도서 수")
                .tag("operation", "borrow")
                .register(meterRegistry);

        bookReturnCounter = Counter.builder("books.returned.total")
                .description("총 반납된 도서 수")
                .tag("operation", "return")
                .register(meterRegistry);

        userLoginCounter = Counter.builder("users.login.total")
                .description("총 사용자 로그인 수")
                .tag("operation", "login")
                .register(meterRegistry);

        userRegistrationCounter = Counter.builder("users.registered.total")
                .description("총 등록된 사용자 수")
                .tag("operation", "registration")
                .register(meterRegistry);

        feedbackSubmissionCounter = Counter.builder("feedback.submitted.total")
                .description("총 제출된 피드백 수")
                .tag("operation", "feedback")
                .register(meterRegistry);

        // 성능 메트릭 타이머 초기화
        bookSearchTimer = Timer.builder("books.search.duration")
                .description("도서 검색 처리 시간")
                .register(meterRegistry);

        bookRecommendationTimer = Timer.builder("books.recommendation.duration")
                .description("도서 추천 처리 시간")
                .register(meterRegistry);

        emailSendingTimer = Timer.builder("email.sending.duration")
                .description("이메일 발송 처리 시간")
                .register(meterRegistry);

        databaseQueryTimer = Timer.builder("database.query.duration")
                .description("데이터베이스 쿼리 처리 시간")
                .register(meterRegistry);

        // 시스템 상태 게이지 초기화
        Gauge.builder("users.active.current")
                .description("현재 활성 사용자 수")
                .register(meterRegistry, this, BusinessMetricsCollector::getActiveUsersCount);

        Gauge.builder("books.total.current")
                .description("현재 전체 도서 수")
                .register(meterRegistry, this, BusinessMetricsCollector::getTotalBooksCount);

        Gauge.builder("books.borrowed.current")
                .description("현재 대여 중인 도서 수")
                .register(meterRegistry, this, BusinessMetricsCollector::getBorrowedBooksCount);

        // 캐시 메트릭 초기화
        Gauge.builder("cache.hit.ratio")
                .description("캐시 히트율")
                .register(meterRegistry, this, BusinessMetricsCollector::getCacheHitRatio);

        log.info("비즈니스 메트릭 수집기 초기화 완료");
    }

    // 비즈니스 이벤트 기록 메서드들
    public void recordBookRegistration() {
        bookRegistrationCounter.increment();
        totalBooksCount.incrementAndGet();
        log.debug("도서 등록 메트릭 기록됨");
    }

    public void recordBookBorrow(String bookId, String userId) {
        bookBorrowCounter.increment();
        borrowedBooksCount.incrementAndGet();
        
        // 도서별 대여 통계를 Redis에 저장
        String key = "book:borrow:stats:" + bookId;
        redisTemplate.opsForValue().increment(key);
        
        log.debug("도서 대여 메트릭 기록됨 - 도서ID: {}, 사용자ID: {}", bookId, userId);
    }

    public void recordBookReturn(String bookId, String userId) {
        bookReturnCounter.increment();
        borrowedBooksCount.decrementAndGet();
        log.debug("도서 반납 메트릭 기록됨 - 도서ID: {}, 사용자ID: {}", bookId, userId);
    }

    public void recordUserLogin(String userId) {
        userLoginCounter.increment();
        
        // 일일 활성 사용자를 Redis Set에 추가
        String dailyActiveUsersKey = "users:active:daily:" + 
            java.time.LocalDate.now().toString();
        redisTemplate.opsForSet().add(dailyActiveUsersKey, userId);
        redisTemplate.expire(dailyActiveUsersKey, Duration.ofDays(1));
        
        log.debug("사용자 로그인 메트릭 기록됨 - 사용자ID: {}", userId);
    }

    public void recordUserRegistration() {
        userRegistrationCounter.increment();
        log.debug("사용자 등록 메트릭 기록됨");
    }

    public void recordFeedbackSubmission(String bookId, Double rating) {
        feedbackSubmissionCounter.increment();
        
        // 평점 분포를 Redis에 저장
        String ratingKey = "feedback:rating:distribution";
        String ratingRange = getRatingRange(rating);
        redisTemplate.opsForHash().increment(ratingKey, ratingRange, 1);
        
        log.debug("피드백 제출 메트릭 기록됨 - 도서ID: {}, 평점: {}", bookId, rating);
    }

    // 성능 메트릭 기록 메서드들
    public Timer.Sample startBookSearchTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordBookSearchTime(Timer.Sample sample, String searchType) {
        sample.stop(Timer.builder("books.search.duration")
                .tag("type", searchType)
                .register(meterRegistry));
    }

    public Timer.Sample startRecommendationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordRecommendationTime(Timer.Sample sample, String algorithmType) {
        sample.stop(Timer.builder("books.recommendation.duration")
                .tag("algorithm", algorithmType)
                .register(meterRegistry));
    }

    public void recordEmailSendingTime(Duration duration, String emailType) {
        Timer.builder("email.sending.duration")
                .tag("type", emailType)
                .register(meterRegistry)
                .record(duration);
    }

    public void recordDatabaseQueryTime(Duration duration, String queryType) {
        Timer.builder("database.query.duration")
                .tag("query_type", queryType)
                .register(meterRegistry)
                .record(duration);
    }

    // 게이지 메트릭 계산 메서드들
    private double getActiveUsersCount() {
        String dailyActiveUsersKey = "users:active:daily:" + 
            java.time.LocalDate.now().toString();
        Long count = redisTemplate.opsForSet().size(dailyActiveUsersKey);
        return count != null ? count.doubleValue() : 0.0;
    }

    private double getTotalBooksCount() {
        return totalBooksCount.doubleValue();
    }

    private double getBorrowedBooksCount() {
        return borrowedBooksCount.doubleValue();
    }

    private double getCacheHitRatio() {
        // Redis 캐시 통계를 통해 히트율 계산
        try {
            String info = (String) redisTemplate.execute(connection -> 
                new String(connection.info("stats")));
            
            if (info != null && info.contains("keyspace_hits") && info.contains("keyspace_misses")) {
                long hits = extractStatValue(info, "keyspace_hits");
                long misses = extractStatValue(info, "keyspace_misses");
                long total = hits + misses;
                
                return total > 0 ? (double) hits / total : 0.0;
            }
        } catch (Exception e) {
            log.warn("캐시 히트율 계산 중 오류 발생: {}", e.getMessage());
        }
        return 0.0;
    }

    private long extractStatValue(String info, String statName) {
        String[] lines = info.split("\n");
        for (String line : lines) {
            if (line.startsWith(statName + ":")) {
                return Long.parseLong(line.split(":")[1].trim());
            }
        }
        return 0;
    }

    private String getRatingRange(Double rating) {
        if (rating == null) return "unknown";
        if (rating <= 1.0) return "1.0";
        if (rating <= 2.0) return "2.0";
        if (rating <= 3.0) return "3.0";
        if (rating <= 4.0) return "4.0";
        return "5.0";
    }

    // 수동으로 게이지 값 업데이트하는 메서드들 (필요시 사용)
    public void updateActiveUsersCount(long count) {
        activeUsersCount.set(count);
    }

    public void updateTotalBooksCount(long count) {
        totalBooksCount.set(count);
    }

    public void updateBorrowedBooksCount(long count) {
        borrowedBooksCount.set(count);
    }
}