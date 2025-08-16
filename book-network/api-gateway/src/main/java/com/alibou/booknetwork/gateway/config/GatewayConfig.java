package com.alibou.booknetwork.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * API Gateway 핵심 설정
 * 
 * Rate Limiting Key Resolver 전략:
 * 1. User-based: 사용자별 개별 제한 (개인화된 서비스)
 * 2. IP-based: IP별 제한 (공개 API, DDoS 방어)
 * 3. Service-based: 서비스별 전역 제한
 * 4. Path-based: 엔드포인트별 차별화된 제한
 * 
 * 설계 원칙:
 * - 공정성: 모든 사용자에게 공평한 리소스 접근
 * - 보안성: 악의적 요청 차단 및 시스템 보호
 * - 가용성: 과부하 상황에서도 서비스 안정성 유지
 * - 확장성: Redis 클러스터 기반 분산 처리
 * 
 * Redis Rate Limiting 알고리즘:
 * - Token Bucket: 버스트 트래픽 허용
 * - 시간 윈도우: 초당 토큰 보충
 * - 분산 동기화: Redis를 통한 클러스터 간 동기화
 */
@Configuration
public class GatewayConfig {

    /**
     * 사용자 기반 Rate Limiting
     * 
     * 사용 사례:
     * - 개인화된 API (추천, 프로필, 알림)
     * - 트랜잭션 API (대여, 반납, 결제)
     * - 사용자별 할당량이 다른 API
     * 
     * 키 생성 전략:
     * 1. JWT 토큰에서 사용자 ID 추출
     * 2. 인증되지 않은 경우 IP 주소 사용
     * 3. 형식: "user:{userId}" 또는 "anonymous:{ip}"
     * 
     * @return 사용자 기반 키 resolver
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // JWT 토큰에서 사용자 ID 추출
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    // JWT 파싱하여 사용자 ID 추출 (실제 구현 필요)
                    String userId = extractUserIdFromJWT(authHeader.substring(7));
                    if (userId != null) {
                        return Mono.just("user:" + userId);
                    }
                } catch (Exception e) {
                    // JWT 파싱 실패 시 IP 기반으로 폴백
                }
            }
            
            // 인증되지 않은 경우 IP 주소 사용
            String clientIP = getClientIP(exchange);
            return Mono.just("anonymous:" + clientIP);
        };
    }

    /**
     * IP 기반 Rate Limiting
     * 
     * 사용 사례:
     * - 공개 API (도서 검색, 카테고리 조회)
     * - DDoS 공격 방어
     * - 지역별 트래픽 제한
     * - 개발자 API 호출 제한
     * 
     * 키 생성 전략:
     * 1. 실제 클라이언트 IP 추출 (프록시 고려)
     * 2. X-Forwarded-For 헤더 파싱
     * 3. 형식: "ip:{실제IP}"
     * 
     * @return IP 기반 키 resolver
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIP = getClientIP(exchange);
            return Mono.just("ip:" + clientIP);
        };
    }

    /**
     * 서비스 기반 Rate Limiting
     * 
     * 사용 사례:
     * - 서비스별 전역 트래픽 제한
     * - 백엔드 서비스 보호
     * - 인프라 리소스 관리
     * - 비용 제어 (외부 API 호출)
     * 
     * @return 서비스 기반 키 resolver
     */
    @Bean
    public KeyResolver serviceKeyResolver() {
        return exchange -> {
            String serviceName = (String) exchange.getAttributes()
                .get(org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR);
            return Mono.just("service:" + (serviceName != null ? serviceName : "unknown"));
        };
    }

    /**
     * 패스 기반 Rate Limiting
     * 
     * 사용 사례:
     * - 엔드포인트별 차별화된 제한
     * - 리소스 집약적 API 보호
     * - 계층화된 서비스 정책
     * - A/B 테스트 트래픽 제어
     * 
     * @return 패스 기반 키 resolver
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getPath().value();
            // 경로를 정규화하여 동적 파라미터 제거
            String normalizedPath = normalizePath(path);
            return Mono.just("path:" + normalizedPath);
        };
    }

    /**
     * 복합 키 기반 Rate Limiting
     * 
     * 사용자 + IP + 서비스 조합으로 더 정교한 제어
     * 
     * @return 복합 키 resolver
     */
    @Bean
    public KeyResolver compositeKeyResolver() {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            String userId = "anonymous";
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String extractedUserId = extractUserIdFromJWT(authHeader.substring(7));
                    if (extractedUserId != null) {
                        userId = extractedUserId;
                    }
                } catch (Exception e) {
                    // JWT 파싱 실패 시 anonymous 유지
                }
            }
            
            String clientIP = getClientIP(exchange);
            String serviceName = (String) exchange.getAttributes()
                .get(org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR);
            
            String compositeKey = String.format("composite:%s:%s:%s", userId, clientIP, serviceName);
            return Mono.just(compositeKey);
        };
    }

    /**
     * 클라이언트 실제 IP 주소 추출
     * 
     * 프록시/로드밸런서 환경 고려:
     * 1. X-Forwarded-For 헤더 우선 확인
     * 2. X-Real-IP 헤더 확인
     * 3. 직접 연결 IP 사용
     * 
     * @param exchange WebExchange 객체
     * @return 실제 클라이언트 IP
     */
    private String getClientIP(org.springframework.web.server.ServerWebExchange exchange) {
        // X-Forwarded-For 헤더 확인 (프록시 환경)
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // 첫 번째 IP가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }
        
        // X-Real-IP 헤더 확인 (Nginx 등)
        String xRealIP = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIP != null && !xRealIP.trim().isEmpty()) {
            return xRealIP.trim();
        }
        
        // 직접 연결 IP 사용
        return Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     * 
     * JWT 구조:
     * Header.Payload.Signature
     * Payload에서 사용자 식별 정보 추출
     * 
     * @param jwt JWT 토큰
     * @return 사용자 ID
     */
    private String extractUserIdFromJWT(String jwt) {
        try {
            // Base64 디코딩 및 JSON 파싱
            String[] chunks = jwt.split("\\.");
            if (chunks.length != 3) {
                return null;
            }
            
            // Payload 디코딩 (실제 JWT 라이브러리 사용 권장)
            String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
            
            // JSON에서 사용자 ID 추출 (간단한 구현)
            if (payload.contains("\"sub\":\"")) {
                int start = payload.indexOf("\"sub\":\"") + 7;
                int end = payload.indexOf("\"", start);
                if (end > start) {
                    return payload.substring(start, end);
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * API 경로 정규화
     * 
     * 동적 파라미터를 제거하여 동일한 엔드포인트로 그룹화
     * 예: /api/users/123 -> /api/users/{id}
     * 
     * @param path 원본 경로
     * @return 정규화된 경로
     */
    private String normalizePath(String path) {
        // 숫자 ID를 {id}로 치환
        String normalized = path.replaceAll("/\\d+", "/{id}");
        
        // UUID 패턴을 {uuid}로 치환
        normalized = normalized.replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "/{uuid}");
        
        // 쿼리 파라미터 제거
        int queryIndex = normalized.indexOf('?');
        if (queryIndex != -1) {
            normalized = normalized.substring(0, queryIndex);
        }
        
        return normalized;
    }
}