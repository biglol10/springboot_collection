package com.alibou.booknetwork.config;

import com.alibou.booknetwork.metrics.MetricsInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.micrometer.core.instrument.binder.db.PostgreSQLDatabaseMetrics;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.io.File;

/**
 * 메트릭 수집 및 모니터링 설정
 * 
 * 이 클래스는 Micrometer와 Prometheus 통합을 통해 포괄적인 메트릭 수집을 구성합니다.
 * 
 * 주요 구성요소:
 * 1. JVM 메트릭 (메모리, GC, 스레드 등)
 * 2. 시스템 메트릭 (CPU, 디스크, 네트워크 등)
 * 3. 데이터베이스 메트릭 (커넥션 풀, 쿼리 성능 등)
 * 4. 캐시 메트릭 (Redis, Caffeine 등)
 * 5. HTTP 요청 메트릭 (응답 시간, 처리량 등)
 * 
 * 왜 필요한가?
 * - 시스템 전반의 성능 가시성 확보
 * - 자원 사용률 최적화
 * - 장애 예방 및 조기 감지
 * - 운영 효율성 향상
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig implements WebMvcConfigurer {

    private final MetricsInterceptor metricsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(metricsInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/v1/actuator/**",
                    "/api/v1/swagger-ui/**",
                    "/api/v1/v3/api-docs/**"
                );
    }

    /**
     * JVM 메모리 메트릭 바인더
     * 힙, 비힙 메모리 사용량 및 가비지 컬렉션 통계를 수집합니다.
     */
    @Bean
    public MeterBinder jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    /**
     * JVM 가비지 컬렉션 메트릭 바인더
     * GC 실행 횟수, 소요 시간, 메모리 회수량을 수집합니다.
     */
    @Bean
    public MeterBinder jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    /**
     * JVM 스레드 메트릭 바인더
     * 활성 스레드 수, 데몬 스레드 수, 피크 스레드 수를 수집합니다.
     */
    @Bean
    public MeterBinder jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    /**
     * JVM 클래스 로더 메트릭 바인더
     * 로드된 클래스 수, 언로드된 클래스 수를 수집합니다.
     */
    @Bean
    public MeterBinder classLoaderMetrics() {
        return new ClassLoaderMetrics();
    }

    /**
     * 시스템 프로세서 메트릭 바인더
     * CPU 사용률, 로드 평균, 프로세서 수를 수집합니다.
     */
    @Bean
    public MeterBinder processorMetrics() {
        return new ProcessorMetrics();
    }

    /**
     * 시스템 업타임 메트릭 바인더
     * 시스템 가동 시간을 수집합니다.
     */
    @Bean
    public MeterBinder uptimeMetrics() {
        return new UptimeMetrics();
    }

    /**
     * 파일 디스크립터 메트릭 바인더
     * 열린 파일 디스크립터 수, 최대 파일 디스크립터 수를 수집합니다.
     */
    @Bean
    public MeterBinder fileDescriptorMetrics() {
        return new FileDescriptorMetrics();
    }

    /**
     * 디스크 공간 메트릭 바인더
     * 디스크 사용량, 여유 공간을 수집합니다.
     */
    @Bean
    public MeterBinder diskSpaceMetrics() {
        return new DiskSpaceMetrics(new File("."));
    }

    /**
     * Logback 로깅 메트릭 바인더
     * 로그 레벨별 이벤트 수를 수집합니다.
     */
    @Bean
    public MeterBinder logbackMetrics() {
        return new LogbackMetrics();
    }

    /**
     * PostgreSQL 데이터베이스 메트릭 바인더
     * 데이터베이스 연결, 트랜잭션, 쿼리 성능 메트릭을 수집합니다.
     */
    @Bean
    public MeterBinder postgreSQLMetrics(DataSource dataSource) {
        return new PostgreSQLDatabaseMetrics(dataSource, "book_social_network");
    }

    /**
     * Redis 연결 팩토리 메트릭 설정
     * Redis 커넥션 풀 및 명령어 실행 메트릭을 활성화합니다.
     */
    @Bean
    public MeterBinder redisMetrics(LettuceConnectionFactory connectionFactory, 
                                   MeterRegistry meterRegistry) {
        return registry -> {
            // Redis 커넥션 풀 메트릭
            registry.gauge("redis.connections.active", connectionFactory, 
                factory -> {
                    try {
                        return factory.getConnection().getNativeConnection() != null ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                });
        };
    }

    /**
     * 캐시 메트릭 바인더 (Caffeine 캐시용)
     * 캐시 히트율, 미스율, 크기 등을 수집합니다.
     */
    @Bean
    public MeterBinder cacheMetrics(CacheManager cacheManager) {
        return registry -> {
            if (cacheManager != null) {
                cacheManager.getCacheNames().forEach(cacheName -> {
                    var cache = cacheManager.getCache(cacheName);
                    if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
                        CaffeineCacheMetrics.monitor(registry, 
                            (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache(), 
                            cacheName);
                    }
                });
            }
        };
    }

    /**
     * 커스텀 애플리케이션 정보 메트릭
     * 애플리케이션 버전, 빌드 정보 등을 수집합니다.
     */
    @Bean
    public MeterBinder applicationInfoMetrics() {
        return registry -> {
            registry.gauge("application.info", 
                "version", getClass().getPackage().getImplementationVersion() != null ? 
                    getClass().getPackage().getImplementationVersion() : "unknown",
                "name", "book-network",
                value -> 1);
            
            // 애플리케이션 시작 시간
            registry.gauge("application.start.time", System.currentTimeMillis());
        };
    }

    /**
     * 비즈니스 메트릭 정의
     * 도서 대여, 사용자 활동 등 비즈니스 관련 메트릭을 수집합니다.
     */
    @Bean
    public MeterBinder businessMetrics() {
        return registry -> {
            // 비즈니스 메트릭은 BusinessMetricsCollector에서 관리
            // 여기서는 시스템 레벨 비즈니스 메트릭만 등록
            
            registry.gauge("business.feature.flags", 
                "feature", "book_recommendation", 
                "enabled", "true",
                value -> 1);
            
            registry.gauge("business.feature.flags", 
                "feature", "real_time_chat", 
                "enabled", "true",
                value -> 1);
        };
    }

    /**
     * 메트릭 필터링 및 최적화 설정
     * 불필요한 메트릭을 제거하고 성능을 최적화합니다.
     */
    @Bean
    public MeterBinder optimizedMetrics() {
        return registry -> {
            // 메트릭 필터 설정
            registry.config().meterFilter(
                io.micrometer.core.instrument.config.MeterFilter.deny(
                    id -> id.getName().startsWith("jvm.buffer") && 
                           id.getName().contains("direct")
                )
            );
            
            // 메트릭 이름 변환
            registry.config().meterFilter(
                io.micrometer.core.instrument.config.MeterFilter.renameTag(
                    "http.server.requests", "uri", "endpoint"
                )
            );
            
            // 높은 카디널리티 태그 제한
            registry.config().meterFilter(
                io.micrometer.core.instrument.config.MeterFilter.maximumAllowableTags(
                    "http.server.requests", "endpoint", 100, 
                    io.micrometer.core.instrument.config.MeterFilter.denyNameStartsWith("endpoint")
                )
            );
        };
    }
}