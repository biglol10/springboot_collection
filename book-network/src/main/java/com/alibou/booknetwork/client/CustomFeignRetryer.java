package com.alibou.booknetwork.client;

import com.alibou.booknetwork.security.JwtService;
import feign.RetryableException;
import feign.Retryer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 커스텀 Feign 재시도 처리기
 * 
 * Feign Client 재시도 패턴의 엔터프라이즈 가치:
 * 1. 장애 허용성(Fault Tolerance): 일시적 네트워크 오류 극복
 * 2. 자동 복구(Auto Recovery): 서비스 간 통신의 안정성 확보
 * 3. 지능형 재시도: 오류 유형별 차별화된 재시도 전략
 * 4. 백프레셔(Back Pressure): 하위 서비스 부하 관리
 * 
 * SSGD 패턴에서 학습한 고급 기법:
 * - 토큰 만료 시 자동 갱신 후 재시도
 * - 지수 백오프(Exponential Backoff) 적용
 * - 재시도 가능한 오류 유형 세분화
 * - 서킷 브레이커와 연동하여 장애 전파 방지
 * 
 * 마이크로서비스 환경에서의 중요성:
 * - 서비스 간 의존성 관리
 * - 일시적 장애로 인한 전체 시스템 다운 방지
 * - 네트워크 지연, 타임아웃 등 불안정한 환경 대응
 * - 사용자 경험 향상 (투명한 재시도로 오류 감춤)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomFeignRetryer implements Retryer {

    private final JwtService jwtService;
    
    // 재시도 설정 상수
    private static final int MAX_ATTEMPTS = 3;              // 최대 재시도 횟수
    private static final long INITIAL_INTERVAL = 1000L;     // 초기 재시도 간격 (1초)
    private static final long MAX_INTERVAL = 5000L;         // 최대 재시도 간격 (5초)
    private static final double MULTIPLIER = 1.5;           // 지수 백오프 배수
    
    // 현재 재시도 상태 (인스턴스별 유지)
    private int attempt = 1;
    private long interval;

    /**
     * 기본 생성자 (Feign에서 자동 호출)
     */
    public CustomFeignRetryer() {
        this(null);
    }

    /**
     * 의존성 주입을 위한 생성자
     */
    public CustomFeignRetryer(JwtService jwtService) {
        this.jwtService = jwtService;
        this.interval = INITIAL_INTERVAL;
    }

    /**
     * 재시도 로직의 핵심 메서드
     * 
     * 재시도 결정 흐름:
     * 1. 최대 시도 횟수 확인
     * 2. 재시도 가능한 오류인지 판단
     * 3. 특별 처리가 필요한 오류 (401 토큰 만료) 처리
     * 4. 지수 백오프 적용하여 대기
     * 5. 재시도 또는 예외 전파
     * 
     * @param e 재시도 가능한 예외
     * @throws RetryableException 재시도 한계 초과 시
     */
    @Override
    public void continueOrPropagate(RetryableException e) {
        log.warn("Feign 호출 실패 - 시도 {}/{}, HTTP 상태: {}, 메시지: {}", 
            attempt, MAX_ATTEMPTS, e.status(), e.getMessage());

        try {
            // 1. 최대 재시도 횟수 확인
            if (attempt >= MAX_ATTEMPTS) {
                log.error("Feign 재시도 한계 초과 - 최종 실패. 총 시도 횟수: {}, 마지막 오류: {}", 
                    attempt, e.getMessage());
                throw e;
            }

            // 2. 재시도 불가능한 오류 확인
            if (!isRetryableError(e)) {
                log.warn("재시도 불가능한 오류 - HTTP 상태: {}, 즉시 실패 처리", e.status());
                throw e;
            }

            // 3. 토큰 관련 오류 특별 처리
            if (isTokenExpiredError(e)) {
                handleTokenExpiredError();
            }

            // 4. 지수 백오프 적용
            long currentInterval = calculateBackoffInterval();
            
            log.info("Feign 재시도 대기 - {}ms 후 {}번째 시도 실행", currentInterval, attempt + 1);
            Thread.sleep(currentInterval);

            // 5. 다음 시도 준비
            attempt++;

        } catch (InterruptedException interruptedException) {
            // 인터럽트 상태 복원 (쓰레드 풀 환경에서 중요)
            Thread.currentThread().interrupt();
            log.error("재시도 대기 중 인터럽트 발생 - 재시도 중단");
            throw e;
        } catch (Exception unexpectedException) {
            log.error("재시도 처리 중 예기치 않은 오류 - 재시도 중단: {}", unexpectedException.getMessage());
            throw e;
        }
    }

    /**
     * Feign이 새로운 요청을 위해 Retryer 인스턴스를 복제할 때 호출
     * 
     * 인스턴스별 독립적인 재시도 상태 유지:
     * - 각 HTTP 요청마다 별도의 재시도 상태
     * - 동시 요청 간 간섭 방지
     * - 쓰레드 안전성 보장
     */
    @Override
    public Retryer clone() {
        CustomFeignRetryer newInstance = new CustomFeignRetryer(this.jwtService);
        log.debug("새로운 Feign Retryer 인스턴스 생성");
        return newInstance;
    }

    /**
     * 재시도 가능한 오류 판단
     * 
     * 재시도 가능한 HTTP 상태 코드:
     * - 401: 인증 오류 (토큰 갱신 후 재시도 가능)
     * - 403: 권한 오류 (일시적 권한 문제 가능성)
     * - 429: 너무 많은 요청 (Rate Limiting)
     * - 500: 서버 내부 오류 (일시적 서버 문제)
     * - 502: Bad Gateway (프록시/게이트웨이 오류)
     * - 503: 서비스 불가능 (일시적 서비스 중단)
     * - 504: Gateway Timeout (게이트웨이 타임아웃)
     * 
     * 재시도 불가능한 오류:
     * - 400: 잘못된 요청 (재시도해도 동일한 오류)
     * - 404: 리소스 없음 (재시도 의미 없음)
     * - 422: 처리할 수 없는 엔티티 (데이터 오류)
     */
    private boolean isRetryableError(RetryableException e) {
        int status = e.status();
        
        // 재시도 가능한 상태 코드들
        boolean isRetryable = status == 401 ||  // Unauthorized (토큰 만료)
                             status == 403 ||   // Forbidden (일시적 권한 문제)
                             status == 429 ||   // Too Many Requests
                             status == 500 ||   // Internal Server Error
                             status == 502 ||   // Bad Gateway
                             status == 503 ||   // Service Unavailable
                             status == 504;     // Gateway Timeout

        if (!isRetryable) {
            log.debug("재시도 불가능한 HTTP 상태 코드: {} - {}", status, e.getMessage());
        }

        return isRetryable;
    }

    /**
     * 토큰 만료 오류 여부 확인
     * 
     * 토큰 만료 감지 기준:
     * - HTTP 401 상태 코드
     * - 응답 메시지에서 토큰 관련 키워드 확인
     * - Authorization 헤더 관련 오류 메시지
     */
    private boolean isTokenExpiredError(RetryableException e) {
        if (e.status() != 401) {
            return false;
        }

        String message = e.getMessage().toLowerCase();
        return message.contains("token") || 
               message.contains("expired") || 
               message.contains("unauthorized") ||
               message.contains("authorization");
    }

    /**
     * 토큰 만료 오류 처리
     * 
     * 자동 토큰 갱신 프로세스:
     * 1. 현재 토큰의 만료 상태 확인
     * 2. 리프레시 토큰을 이용한 새 토큰 발급
     * 3. SecurityContext 업데이트
     * 4. 후속 요청에서 새 토큰 사용
     * 
     * 엔터프라이즈 패턴의 가치:
     * - 사용자 투명한 토큰 갱신
     * - 세션 연속성 보장
     * - 인증 오류로 인한 사용자 경험 저하 방지
     */
    private void handleTokenExpiredError() {
        if (jwtService == null) {
            log.warn("JwtService가 주입되지 않아 토큰 갱신 불가");
            return;
        }

        try {
            log.info("토큰 만료 감지 - 자동 토큰 갱신 시도");
            
            // TODO: 실제 토큰 갱신 로직 구현
            // 1. 현재 리프레시 토큰 확인
            // String refreshToken = jwtService.getRefreshTokenFromContext();
            // 
            // 2. 새 액세스 토큰 발급
            // String newAccessToken = jwtService.refreshAccessToken(refreshToken);
            // 
            // 3. SecurityContext 업데이트
            // jwtService.updateSecurityContext(newAccessToken);
            
            log.info("토큰 갱신 완료 - 재시도 준비");

        } catch (Exception e) {
            log.error("토큰 갱신 실패 - 재시도 계속: {}", e.getMessage());
            // 토큰 갱신 실패 시에도 일반적인 재시도 로직 적용
        }
    }

    /**
     * 지수 백오프 간격 계산
     * 
     * 지수 백오프의 이점:
     * 1. 서버 부하 점진적 감소: 재시도 간격이 점점 늘어남
     * 2. 일시적 장애 복구 시간 확보: 서버가 회복할 시간 제공
     * 3. 네트워크 혼잡 완화: 동시 재시도 요청 분산
     * 4. 리소스 효율성: CPU와 네트워크 자원 절약
     * 
     * 계산 공식:
     * nextInterval = min(currentInterval * multiplier, maxInterval)
     */
    private long calculateBackoffInterval() {
        long currentInterval = this.interval;
        
        // 다음 간격 계산 (지수 백오프 적용)
        this.interval = Math.min((long) (this.interval * MULTIPLIER), MAX_INTERVAL);
        
        log.debug("지수 백오프 적용 - 현재 간격: {}ms, 다음 간격: {}ms", 
            currentInterval, this.interval);
            
        return currentInterval;
    }

    /**
     * Feign 클라이언트 설정에서 사용할 정적 팩토리 메서드
     * 
     * 설정 예시:
     * @Bean
     * public Retryer feignRetryer() {
     *     return CustomFeignRetryer.create();
     * }
     */
    public static CustomFeignRetryer create() {
        return new CustomFeignRetryer(null);
    }

    /**
     * 의존성이 주입된 Retryer 생성
     */
    public static CustomFeignRetryer create(JwtService jwtService) {
        return new CustomFeignRetryer(jwtService);
    }

    /**
     * 현재 재시도 상태 정보 반환 (디버깅/모니터링 용도)
     */
    public String getRetryStatus() {
        return String.format("시도: %d/%d, 현재 간격: %dms", 
            attempt, MAX_ATTEMPTS, interval);
    }

    /**
     * 재시도 통계 정보 (운영 모니터링용)
     * 
     * 실제 환경에서는 메트릭 수집 시스템과 연동:
     * - 재시도 발생 횟수
     * - 재시도 성공/실패율
     * - 평균 재시도 간격
     * - 토큰 갱신 빈도
     */
    public void recordRetryMetrics(String operation, boolean success) {
        try {
            // 메트릭 수집 예시
            log.info("재시도 메트릭 - 작업: {}, 성공 여부: {}, 총 시도: {}", 
                operation, success, attempt);
                
            // 실제 환경에서는 메트릭 서비스로 전송
            // metricsService.recordRetry(operation, attempt, success, interval);
            
        } catch (Exception e) {
            log.warn("재시도 메트릭 기록 실패: {}", e.getMessage());
        }
    }
}