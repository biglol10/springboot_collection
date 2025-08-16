package com.alibou.booknetwork.websocket;

import com.alibou.booknetwork.security.JwtService;
import com.alibou.booknetwork.user.User;
import com.alibou.booknetwork.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 인증 인터셉터
 * 
 * WebSocket 보안의 핵심 과제:
 * 1. 초기 핸드셰이크 시점에서 인증 수행
 * 2. 연결 후 지속적인 인증 상태 유지
 * 3. 토큰 만료 시 자동 재인증 또는 연결 종료
 * 4. 권한 기반 채팅방 접근 제어
 * 
 * HTTP vs WebSocket 인증 차이점:
 * - HTTP: 요청마다 토큰 검증
 * - WebSocket: 핸드셰이크 시 한 번 검증 후 세션 유지
 * - 보안 위험: 장시간 연결로 토큰 만료 감지 어려움
 * - 해결책: 주기적 토큰 검증 + 자동 재연결
 * 
 * SSGD 보안 패턴 적용:
 * - JWT 토큰 기반 인증
 * - IP 주소 검증 및 로깅
 * - 사용자 정보 세션 속성 저장
 * - 실패 시 상세 로깅 및 모니터링
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * WebSocket 핸드셰이크 시작 전 인증 처리
     * 
     * 인증 프로세스:
     * 1. Authorization 헤더에서 JWT 토큰 추출
     * 2. 토큰 유효성 검증 (서명, 만료시간, 발급자)
     * 3. 토큰에서 사용자 정보 추출
     * 4. 데이터베이스에서 사용자 존재 여부 확인
     * 5. 사용자 정보를 WebSocket 세션 속성에 저장
     * 
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param wsHandler WebSocket 핸들러
     * @param attributes WebSocket 세션 속성 (사용자 정보 저장)
     * @return 인증 성공 시 true, 실패 시 false
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
        
        try {
            log.debug("WebSocket 핸드셰이크 인증 시작 - URI: {}", request.getURI());
            
            // 1. Authorization 헤더에서 JWT 토큰 추출
            String token = extractTokenFromRequest(request);
            if (token == null) {
                log.warn("WebSocket 인증 실패 - JWT 토큰 없음, IP: {}", getClientIP(request));
                return false;
            }

            // 2. JWT 토큰 유효성 검증
            if (!jwtService.isTokenValid(token)) {
                log.warn("WebSocket 인증 실패 - 유효하지 않은 토큰, IP: {}", getClientIP(request));
                return false;
            }

            // 3. 토큰에서 사용자 이메일 추출
            String userEmail = jwtService.extractUsername(token);
            if (userEmail == null) {
                log.warn("WebSocket 인증 실패 - 사용자 이메일 추출 실패, IP: {}", getClientIP(request));
                return false;
            }

            // 4. 데이터베이스에서 사용자 조회
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user == null) {
                log.warn("WebSocket 인증 실패 - 사용자 없음, 이메일: {}, IP: {}", 
                    userEmail, getClientIP(request));
                return false;
            }

            // 5. 사용자 정보를 WebSocket 세션 속성에 저장
            attributes.put("user", user);
            attributes.put("userId", user.getId());
            attributes.put("userEmail", user.getEmail());
            attributes.put("userName", user.getFullName());
            attributes.put("clientIP", getClientIP(request));
            attributes.put("connectTime", System.currentTimeMillis());
            attributes.put("token", token);

            // 6. 성공 로깅
            log.info("WebSocket 인증 성공 - 사용자: {}, IP: {}", user.getEmail(), getClientIP(request));
            return true;

        } catch (Exception e) {
            log.error("WebSocket 인증 처리 중 오류 발생 - IP: {}, 오류: {}", 
                getClientIP(request), e.getMessage(), e);
            return false;
        }
    }

    /**
     * WebSocket 핸드셰이크 완료 후 처리
     * 
     * 후처리 작업:
     * 1. 연결 성공/실패 로깅
     * 2. 연결 통계 업데이트
     * 3. 모니터링 메트릭 수집
     * 4. 사용자 온라인 상태 업데이트
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler wsHandler, Exception exception) {
        
        if (exception != null) {
            log.error("WebSocket 핸드셰이크 실패 - URI: {}, IP: {}, 오류: {}", 
                request.getURI(), getClientIP(request), exception.getMessage());
        } else {
            log.info("WebSocket 핸드셰이크 완료 - URI: {}, IP: {}", 
                request.getURI(), getClientIP(request));
                
            // TODO: 연결 통계 업데이트
            // connectionStatsService.incrementActiveConnections();
            
            // TODO: 사용자 온라인 상태 업데이트
            // userPresenceService.setUserOnline(userId);
        }
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     * 
     * 토큰 추출 방법:
     * 1. Authorization 헤더: "Bearer {token}" 형태
     * 2. 쿼리 파라미터: ?token={token} 형태 (WebSocket 특성상 허용)
     * 3. 쿠키: HttpOnly 쿠키에서 추출 (보안성 높음)
     * 
     * 우선순위: Authorization 헤더 > 쿼리 파라미터 > 쿠키
     */
    private String extractTokenFromRequest(ServerHttpRequest request) {
        try {
            // 1. Authorization 헤더에서 추출
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }

            // 2. 쿼리 파라미터에서 추출 (WebSocket 연결 시 헤더 설정이 어려운 경우)
            URI uri = request.getURI();
            String query = uri.getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                        return keyValue[1];
                    }
                }
            }

            // 3. 쿠키에서 추출 (추후 구현 가능)
            // Cookie[] cookies = request.getCookies();
            // if (cookies != null) {
            //     for (Cookie cookie : cookies) {
            //         if ("authToken".equals(cookie.getName())) {
            //             return cookie.getValue();
            //         }
            //     }
            // }

            return null;

        } catch (Exception e) {
            log.warn("토큰 추출 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     * 
     * 프록시/로드밸런서 환경에서 실제 클라이언트 IP 추출:
     * - X-Forwarded-For: 프록시 체인을 거친 원본 IP
     * - X-Real-IP: nginx 등에서 설정하는 실제 IP
     * - 보안 로깅 및 접근 제어에 활용
     */
    private String getClientIP(ServerHttpRequest request) {
        try {
            // X-Forwarded-For 헤더 확인 (프록시 환경)
            String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
                // 첫 번째 IP가 실제 클라이언트 IP
                return xForwardedFor.split(",")[0].trim();
            }

            // X-Real-IP 헤더 확인 (nginx 등)
            String xRealIP = request.getHeaders().getFirst("X-Real-IP");
            if (xRealIP != null && !xRealIP.trim().isEmpty()) {
                return xRealIP.trim();
            }

            // 직접 연결인 경우 RemoteAddress 사용
            if (request.getRemoteAddress() != null) {
                return request.getRemoteAddress().getAddress().getHostAddress();
            }

            return "unknown";

        } catch (Exception e) {
            log.warn("클라이언트 IP 추출 중 오류 발생: {}", e.getMessage());
            return "unknown";
        }
    }
}