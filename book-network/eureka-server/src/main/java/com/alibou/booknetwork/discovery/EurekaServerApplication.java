package com.alibou.booknetwork.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * 서비스 디스커버리 서버 (Eureka Server)
 * 
 * 마이크로서비스 아키텍처의 핵심 인프라 컴포넌트:
 * 1. 서비스 등록: 마이크로서비스들이 자신의 위치 정보 등록
 * 2. 서비스 발견: 클라이언트가 필요한 서비스의 위치 정보 조회
 * 3. 헬스 체크: 서비스의 상태를 지속적으로 모니터링
 * 4. 로드 밸런싱: 여러 인스턴스 간 부하 분산 지원
 * 
 * Eureka의 엔터프라이즈 가치:
 * - 자동 서비스 발견: 수동 설정 없이 서비스 간 통신
 * - 장애 격리: 비정상 서비스 자동 제외
 * - 확장성: 서비스 인스턴스 동적 추가/제거
 * - 고가용성: 여러 Eureka 서버로 클러스터 구성 가능
 * 
 * Netflix OSS 검증된 패턴:
 * - Netflix에서 수년간 대규모 운영 검증
 * - 초당 수백만 요청 처리 가능
 * - 장애 시나리오별 대응 로직 내장
 * - AP (Availability-Partition tolerance) 일관성 모델
 * 
 * 운영 고려사항:
 * - 서비스 등록/해제 지연: 기본 30초
 * - 하트비트 주기: 30초마다 상태 확인
 * - 캐시 갱신: 클라이언트 캐시 30초마다 갱신
 * - 자체 보호 모드: 네트워크 분할 상황 대응
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    /**
     * 서비스 디스커버리 서버 시작점
     * 
     * 시작 시 수행되는 초기화:
     * 1. Eureka 서버 콘솔 대시보드 활성화 (기본 포트: 8761)
     * 2. 레지스트리 저장소 초기화
     * 3. 피어 서버 연결 (클러스터 모드인 경우)
     * 4. 자체 보호 모드 활성화
     * 5. 메트릭 수집 시작
     * 
     * @param args 애플리케이션 시작 인자
     */
    public static void main(String[] args) {
        System.out.println("""
            🚀 Starting Book Network Service Discovery Server...
            
            ┌─────────────────────────────────────────────────┐
            │              EUREKA SERVER                      │
            │                                                 │
            │  📍 Service Registry & Discovery                │
            │  🔍 Health Monitoring                          │
            │  ⚖️  Load Balancing Support                    │
            │  🛡️  Self-Protection Mode                      │
            │                                                 │
            │  Dashboard: http://localhost:8761              │
            │  Status: http://localhost:8761/actuator/health │
            └─────────────────────────────────────────────────┘
            """);
            
        SpringApplication.run(EurekaServerApplication.class, args);
        
        System.out.println("""
            ✅ Eureka Server started successfully!
            
            📊 Monitoring endpoints:
            • Health Check: http://localhost:8761/actuator/health
            • Metrics: http://localhost:8761/actuator/metrics
            • Prometheus: http://localhost:8761/actuator/prometheus
            
            🎯 Ready to accept service registrations!
            """);
    }
}