package com.alibou.booknetwork.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정 클래스
 * 
 * 핵심 설계 원칙:
 * 1. 적절한 쓰레드 풀 크기 설정으로 자원 효율성 확보
 * 2. 큐 크기 제한을 통한 메모리 보호
 * 3. 거부 정책을 통한 시스템 안정성 보장
 * 4. 예외 처리를 통한 장애 격리
 * 
 * SSGD에서 학습한 엔터프라이즈 패턴:
 * - 도메인별 쓰레드 풀 분리 (이메일, 알림, 일반 비동기)
 * - ThreadPoolTaskExecutor의 세밀한 튜닝
 * - 비동기 예외 처리 전략
 * - JMX를 통한 모니터링 지원
 * 
 * 성능 고려사항:
 * - CPU 집약적 vs I/O 집약적 작업 구분
 * - 쓰레드 풀 크기 = CPU 코어 수 × (1 + 대기시간/처리시간)
 * - 큐 크기는 메모리와 응답성의 트레이드오프
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 이메일 전용 쓰레드 풀
     * 
     * 설계 근거:
     * - 이메일 발송은 I/O 집약적 작업 (SMTP 서버 통신)
     * - 대기 시간이 길어 상대적으로 많은 쓰레드 필요
     * - 이메일 발송 실패가 다른 비동기 작업에 영향 주지 않도록 분리
     * 
     * 파라미터 설정 근거:
     * - corePoolSize: 기본 5개 쓰레드로 일반적인 이메일 발송 처리
     * - maxPoolSize: 최대 15개까지 확장하여 트래픽 급증 대응
     * - queueCapacity: 100개 큐로 버스트 트래픽 흡수
     * - keepAliveSeconds: 300초 후 여유 쓰레드 정리하여 자원 절약
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 쓰레드 풀 크기 (항상 유지되는 쓰레드 수)
        executor.setCorePoolSize(5);
        
        // 최대 쓰레드 풀 크기 (부하 시 확장 가능한 최대 쓰레드 수)
        executor.setMaxPoolSize(15);
        
        // 큐 용량 (대기 중인 작업을 저장할 큐의 크기)
        executor.setQueueCapacity(100);
        
        // 쓰레드 유지 시간 (최대 풀 크기까지 생성된 여유 쓰레드의 생존 시간)
        executor.setKeepAliveSeconds(300);
        
        // 쓰레드 이름 접두사 (디버깅 및 모니터링 시 식별 용이)
        executor.setThreadNamePrefix("Email-Async-");
        
        // 애플리케이션 종료 시 실행 중인 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        // 거부 정책: 큐가 가득 찰 때의 처리 방식
        // CallerRunsPolicy: 호출한 쓰레드에서 직접 실행 (백프레셔 효과)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 예외 발생 시 로깅
        executor.setTaskDecorator(runnable -> () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("이메일 비동기 작업 중 예외 발생", e);
                throw e;
            }
        });
        
        executor.initialize();
        
        log.info("이메일 TaskExecutor 초기화 완료 - Core: {}, Max: {}, Queue: {}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
            
        return executor;
    }

    /**
     * 알림 전용 쓰레드 풀
     * 
     * 알림 시스템 특성:
     * - 실시간성이 중요 (낮은 지연시간 요구)
     * - 대량 처리 가능성 (대량 푸시 알림)
     * - 외부 서비스 의존성 (푸시 서비스, 웹소켓)
     * 
     * 이메일보다 더 많은 쓰레드 할당 이유:
     * - 알림은 이메일보다 빈번하게 발생
     * - 실시간 알림의 즉시성 요구사항
     * - 배치 알림 처리 시 병렬성 필요
     */
    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(8);      // 기본 8개 쓰레드
        executor.setMaxPoolSize(25);      // 최대 25개까지 확장
        executor.setQueueCapacity(200);   // 200개 큐 (대량 알림 대응)
        executor.setKeepAliveSeconds(180);
        executor.setThreadNamePrefix("Notification-Async-");
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        // 알림은 중요하므로 거부 시에도 처리 보장
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setTaskDecorator(runnable -> () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("알림 비동기 작업 중 예외 발생", e);
                // 알림 실패 시 대체 수단 활용 가능
                // 예: 실패한 알림을 데이터베이스에 저장 후 재처리
            }
        });
        
        executor.initialize();
        
        log.info("알림 TaskExecutor 초기화 완료 - Core: {}, Max: {}, Queue: {}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
            
        return executor;
    }

    /**
     * 범용 비동기 작업용 쓰레드 풀
     * 
     * 용도:
     * - 파일 업로드/다운로드
     * - 데이터 분석 및 리포트 생성
     * - 외부 API 호출
     * - 기타 일반적인 비동기 작업
     * 
     * CPU 집약적 작업 고려:
     * - corePoolSize를 CPU 코어 수 기준으로 설정
     * - I/O 집약적 작업보다 적은 쓰레드로 운영
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Runtime.getRuntime().availableProcessors()로 동적 설정도 가능
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        
        executor.setCorePoolSize(availableProcessors);           // CPU 코어 수
        executor.setMaxPoolSize(availableProcessors * 2);       // CPU 코어 수 × 2
        executor.setQueueCapacity(50);                          // 적은 큐 (빠른 응답성)
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("General-Async-");
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setTaskDecorator(runnable -> () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("범용 비동기 작업 중 예외 발생", e);
                throw e;
            }
        });
        
        executor.initialize();
        
        log.info("범용 TaskExecutor 초기화 완료 - Core: {}, Max: {}, Queue: {} (CPU 코어: {})", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
            availableProcessors);
            
        return executor;
    }

    /**
     * 기본 비동기 실행자 지정
     * 
     * @Async 어노테이션에 별도 실행자를 지정하지 않을 때 사용
     * 범용 taskExecutor를 기본값으로 설정
     */
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    /**
     * 비동기 작업 중 발생한 예외 처리
     * 
     * 엔터프라이즈 예외 처리 전략:
     * 1. 모든 예외를 로깅하여 추적 가능성 확보
     * 2. 예외 타입별 차별화된 처리
     * 3. 모니터링 시스템과의 연동 고려
     * 4. 사용자에게 적절한 피드백 제공 방안
     * 
     * 실제 운영환경에서는:
     * - 예외를 모니터링 시스템으로 전송 (예: Sentry, DataDog)
     * - 중요한 예외는 즉시 알림 발송
     * - 예외 발생률을 메트릭으로 수집
     * - 재시도 로직 구현 고려
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            log.error("비동기 작업 중 처리되지 않은 예외 발생");
            log.error("메서드: {}", method.getName());
            log.error("매개변수: {}", java.util.Arrays.toString(objects));
            log.error("예외 내용: ", throwable);
            
            // 실제 환경에서는 여기서 추가 처리
            // 1. 모니터링 시스템에 예외 전송
            // 2. 중요 예외의 경우 즉시 알림
            // 3. 재시도 큐에 작업 재등록 고려
            // 4. 예외 통계 수집
            
            // 예시: 모니터링 서비스 연동
            // monitoringService.reportException(throwable, method, objects);
            
            // 예시: 슬랙 알림 발송
            // if (isCriticalException(throwable)) {
            //     slackNotificationService.sendAlert("비동기 작업 중 심각한 예외 발생", throwable);
            // }
        };
    }
}