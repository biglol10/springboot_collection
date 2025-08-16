package com.alibou.booknetwork.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * 엔터프라이즈 API Gateway
 * 
 * Spring Cloud Gateway의 핵심 기능:
 * 1. 라우팅: 클라이언트 요청을 적절한 마이크로서비스로 전달
 * 2. 로드 밸런싱: 여러 서비스 인스턴스 간 부하 분산
 * 3. 인증/인가: 중앙화된 보안 검증
 * 4. Rate Limiting: API 호출 빈도 제한
 * 5. Circuit Breaker: 장애 전파 차단
 * 6. 모니터링: 요청/응답 추적 및 메트릭 수집
 * 
 * API Gateway의 엔터프라이즈 가치:
 * - 단일 진입점: 클라이언트 복잡성 감소
 * - 보안 중앙화: 일관된 인증/인가 정책
 * - 서비스 추상화: 백엔드 변경에 대한 클라이언트 격리
 * - 트래픽 제어: Rate limiting, 캐싱, 압축
 * - 관측성: 중앙화된 로깅, 메트릭, 추적
 * 
 * Spring Cloud Gateway vs Zuul:
 * - 비동기/논블로킹: WebFlux 기반 고성능
 * - Spring 5 생태계: 최신 스프링 스택 완벽 지원
 * - 네이티브 Cloud 지원: Kubernetes, Docker 최적화
 * - 확장성: 대용량 트래픽 처리 능력
 * 
 * 성능 특성:
 * - 처리량: Zuul 대비 2-3배 향상
 * - 메모리: 더 효율적인 리소스 사용
 * - 응답시간: 논블로킹 I/O로 지연시간 감소
 * - 확장성: 수직/수평 확장 모두 용이
 */
@SpringBootApplication
@EnableEurekaClient
public class ApiGatewayApplication {

    /**
     * API Gateway 애플리케이션 시작점
     * 
     * 시작 시 초기화되는 컴포넌트:
     * 1. Route Predicates: URL 패턴 매칭 규칙
     * 2. Gateway Filters: 요청/응답 처리 필터 체인
     * 3. Load Balancer: 서비스 인스턴스 선택 로직
     * 4. Circuit Breaker: 장애 감지 및 차단 로직
     * 5. Security Filters: JWT 검증 및 권한 확인
     * 6. Rate Limiting: Redis 기반 속도 제한
     * 7. Metrics Collection: Prometheus 메트릭 수집
     * 
     * @param args 애플리케이션 시작 인자
     */
    public static void main(String[] args) {
        System.out.println("""
            🚀 Starting Book Network API Gateway...
            
            ┌─────────────────────────────────────────────────┐
            │                API GATEWAY                      │
            │                                                 │
            │  🛣️  Intelligent Routing                       │
            │  ⚖️  Load Balancing                            │
            │  🔐 Authentication & Authorization             │
            │  🚧 Rate Limiting                              │
            │  🛡️  Circuit Breaker                          │
            │  📊 Monitoring & Tracing                       │
            │                                                 │
            │  Gateway: http://localhost:8080                │
            │  Health: http://localhost:8080/actuator/health │
            └─────────────────────────────────────────────────┘
            """);
            
        SpringApplication.run(ApiGatewayApplication.class, args);
        
        System.out.println("""
            ✅ API Gateway started successfully!
            
            🎯 Available Services:
            • User Service: /api/users/**
            • Book Service: /api/books/**
            • Lending Service: /api/lending/**
            • Recommendation Service: /api/recommendations/**
            • Notification Service: /api/notifications/**
            • Chat Service: /api/chat/**
            • Analytics Service: /api/analytics/**
            
            📊 Monitoring endpoints:
            • Health: /actuator/health
            • Routes: /actuator/gateway/routes
            • Metrics: /actuator/metrics
            • Prometheus: /actuator/prometheus
            
            🚀 Ready to route requests!
            """);
    }
}