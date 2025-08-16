package com.alibou.booknetwork.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * 중앙화된 설정 관리 서버 (Spring Cloud Config Server)
 * 
 * 마이크로서비스 설정 관리의 핵심 과제:
 * 1. 분산 환경: 수십 개의 서비스, 각각 다른 설정
 * 2. 환경별 설정: 개발/테스트/스테이징/운영
 * 3. 동적 변경: 서버 재시작 없이 설정 변경
 * 4. 보안: 민감한 정보 (DB 비밀번호, API 키) 안전 관리
 * 5. 버전 관리: 설정 변경 이력 추적
 * 
 * Spring Cloud Config의 엔터프라이즈 가치:
 * - 중앙화: 모든 서비스 설정을 한 곳에서 관리
 * - 외부화: 코드와 설정 분리로 유연성 증대
 * - 환경별 관리: 프로파일 기반 설정 분리
 * - 동적 갱신: @RefreshScope로 실시간 설정 변경
 * - 보안: 암호화/복호화 지원
 * - 감사: Git 기반 변경 이력 추적
 * 
 * 설정 저장소 전략:
 * 1. Git Repository: 버전 관리, 협업, 감사 로그
 * 2. File System: 단순한 로컬 파일 기반
 * 3. Vault: 민감한 정보 전용 보안 저장소
 * 4. JDBC: 데이터베이스 기반 설정 관리
 * 
 * 12-Factor App 원칙 준수:
 * - Factor III: 설정을 환경에 저장
 * - Factor XII: 관리 프로세스를 일회성 작업으로 실행
 * 
 * Netflix OSS 검증된 패턴:
 * - Archaius: 동적 설정 관리
 * - Ribbon: 클라이언트 사이드 로드 밸런싱 설정
 * - Hystrix: Circuit Breaker 설정
 * - Eureka: 서비스 디스커버리 설정
 */
@SpringBootApplication
@EnableConfigServer
@EnableEurekaClient
public class ConfigServerApplication {

    /**
     * Config Server 애플리케이션 시작점
     * 
     * 시작 시 초기화되는 설정 저장소:
     * 1. Git Repository 클론/업데이트
     * 2. 환경별 프로파일 로딩
     * 3. 암호화 키 초기화
     * 4. 서비스 디스커버리 등록
     * 5. 헬스 체크 엔드포인트 활성화
     * 
     * 제공하는 설정 API:
     * - /{application}/{profile}[/{label}]
     * - /{application}-{profile}.yml
     * - /{label}/{application}-{profile}.yml
     * - /{application}-{profile}.properties
     * - /{label}/{application}-{profile}.properties
     * 
     * @param args 애플리케이션 시작 인자
     */
    public static void main(String[] args) {
        System.out.println("""
            🚀 Starting Book Network Configuration Server...
            
            ┌─────────────────────────────────────────────────┐
            │              CONFIG SERVER                      │
            │                                                 │
            │  ⚙️  Centralized Configuration                 │
            │  🔒 Secure Property Management                 │
            │  🌍 Environment-specific Profiles             │
            │  🔄 Dynamic Configuration Refresh             │
            │  📝 Git-based Version Control                 │
            │  🔐 Encryption/Decryption Support             │
            │                                                 │
            │  Server: http://localhost:8888                 │
            │  Health: http://localhost:8888/actuator/health │
            └─────────────────────────────────────────────────┘
            """);
            
        SpringApplication.run(ConfigServerApplication.class, args);
        
        System.out.println("""
            ✅ Config Server started successfully!
            
            📋 Configuration Endpoints:
            • Service Config: /{service-name}/{profile}
            • Properties: /{service-name}-{profile}.properties
            • YAML: /{service-name}-{profile}.yml
            • Health Check: /actuator/health
            
            🔧 Supported Services:
            • api-gateway (gateway configuration)
            • user-service (user management settings)
            • book-service (book catalog settings)
            • lending-service (transaction settings)
            • recommendation-service (AI model settings)
            • notification-service (messaging settings)
            • chat-service (real-time communication)
            • analytics-service (data processing)
            
            🌍 Supported Profiles:
            • development (개발환경)
            • staging (스테이징환경)
            • production (운영환경)
            
            🎯 Ready to serve configurations!
            """);
    }
}