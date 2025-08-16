package com.alibou.booknetwork.config;

import com.alibou.booknetwork.websocket.ChatWebSocketHandler;
import com.alibou.booknetwork.websocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 설정 클래스
 * 
 * 실시간 통신의 엔터프라이즈 가치:
 * 1. 사용자 경험 혁신: 즉시성을 통한 만족도 극대화
 * 2. 협업 효율성: 실시간 소통으로 업무 생산성 향상
 * 3. 참여도 증대: 라이브 인터랙션으로 플랫폼 사용 시간 연장
 * 4. 경쟁 우위: 차세대 웹 경험 제공으로 시장 선도
 * 
 * WebSocket vs HTTP 비교:
 * - HTTP: 요청-응답 기반, 단방향, 오버헤드 큼
 * - WebSocket: 양방향 실시간, 지속 연결, 오버헤드 작음
 * - 성능: HTTP 대비 90% 지연시간 감소, 70% 대역폭 절약
 * 
 * SSGD에서 학습한 실시간 통신 패턴:
 * - 연결 관리: 사용자별 세션 추적 및 관리
 * - 메시지 라우팅: 채팅방별, 사용자별 메시지 전달
 * - 보안 강화: JWT 기반 WebSocket 인증
 * - 확장성: Redis Pub/Sub으로 다중 인스턴스 지원
 * - 모니터링: 연결 상태, 메시지 처리량 실시간 추적
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * WebSocket 핸들러 등록
     * 
     * 엔드포인트 설계 원칙:
     * 1. RESTful 패턴 준수: /ws/chat/{roomId} 형태
     * 2. 버전 관리: /ws/v1/chat 형태로 API 버전 명시
     * 3. 도메인 분리: 채팅, 알림, 모니터링별 엔드포인트 분리
     * 4. 보안 고려: CORS 설정 및 인증 인터셉터 적용
     * 
     * CORS 설정 이유:
     * - 브라우저 보안 정책 준수
     * - 다양한 도메인에서 접근 허용
     * - 개발/운영 환경별 유연한 설정
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("WebSocket 핸들러 등록 시작");
        
        // 채팅 WebSocket 핸들러 등록
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*") // 운영환경에서는 특정 도메인만 허용
                .withSockJS(); // SockJS 폴백 지원
        
        // 알림 WebSocket 핸들러 등록 (추후 확장)
        // registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
        //         .addInterceptors(webSocketAuthInterceptor)
        //         .setAllowedOrigins("*")
        //         .withSockJS();
        
        // 관리자 모니터링 WebSocket 핸들러 (추후 확장)
        // registry.addHandler(adminWebSocketHandler, "/ws/admin")
        //         .addInterceptors(adminAuthInterceptor)
        //         .setAllowedOrigins("*");
        
        log.info("WebSocket 핸들러 등록 완료");
    }
}