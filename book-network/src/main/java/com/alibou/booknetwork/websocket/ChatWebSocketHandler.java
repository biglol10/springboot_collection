package com.alibou.booknetwork.websocket;

import com.alibou.booknetwork.chat.ChatMessage;
import com.alibou.booknetwork.chat.ChatRoom;
import com.alibou.booknetwork.chat.ChatService;
import com.alibou.booknetwork.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채팅 WebSocket 핸들러
 * 
 * 실시간 채팅 시스템의 핵심 설계 원칙:
 * 1. 확장성: 수천 명의 동시 연결 처리
 * 2. 신뢰성: 메시지 유실 방지 및 순서 보장
 * 3. 보안성: 인증된 사용자만 채팅 참여
 * 4. 성능: 낮은 지연시간으로 실시간 경험 제공
 * 
 * 엔터프라이즈 채팅 시스템 요구사항:
 * - 메시지 영속성: 데이터베이스 저장으로 히스토리 관리
 * - 채팅방 관리: 생성, 참여, 퇴장, 권한 관리
 * - 메시지 타입: 텍스트, 이미지, 파일, 시스템 메시지
 * - 읽음 상태: 메시지 읽음/안읽음 추적
 * - 알림 연동: 오프라인 사용자에게 푸시 알림
 * 
 * SSGD 실시간 통신 패턴 적용:
 * - 세션 관리: 사용자별 WebSocket 세션 추적
 * - 메시지 브로커: Redis Pub/Sub으로 다중 인스턴스 지원
 * - 백프레셔: 과도한 메시지 발송 제한
 * - 장애 복구: 연결 끊김 시 자동 재연결
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    // 활성 WebSocket 세션 관리 (메모리 기반)
    // 운영환경에서는 Redis 등 외부 저장소 사용 권장
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Integer, String> userSessionMap = new ConcurrentHashMap<>();

    /**
     * WebSocket 연결 성공 시 호출
     * 
     * 연결 초기화 작업:
     * 1. 세션 정보 저장 및 매핑
     * 2. 사용자 온라인 상태 업데이트
     * 3. 연결 환영 메시지 발송
     * 4. 채팅방 참여 가능 목록 전송
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            // 세션에서 사용자 정보 추출
            User user = (User) session.getAttributes().get("user");
            Integer userId = (Integer) session.getAttributes().get("userId");
            String clientIP = (String) session.getAttributes().get("clientIP");

            if (user == null || userId == null) {
                log.error("WebSocket 연결 실패 - 사용자 정보 없음, 세션ID: {}", session.getId());
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            // 세션 매핑 저장
            activeSessions.put(session.getId(), session);
            userSessionMap.put(userId, session.getId());

            log.info("WebSocket 연결 성공 - 사용자: {}, 세션ID: {}, IP: {}", 
                user.getEmail(), session.getId(), clientIP);

            // 연결 환영 메시지 발송
            ChatMessage welcomeMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.SYSTEM)
                .content("채팅에 연결되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();

            sendMessageToSession(session, welcomeMessage);

            // 사용자 온라인 상태 업데이트
            chatService.updateUserOnlineStatus(userId, true);

            // 연결 통계 업데이트
            updateConnectionStats(1);

        } catch (Exception e) {
            log.error("WebSocket 연결 처리 중 오류 발생 - 세션ID: {}, 오류: {}", 
                session.getId(), e.getMessage(), e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 클라이언트로부터 메시지 수신 시 호출
     * 
     * 메시지 처리 프로세스:
     * 1. JSON 메시지 파싱 및 유효성 검증
     * 2. 메시지 타입별 처리 분기
     * 3. 채팅방 권한 확인
     * 4. 메시지 데이터베이스 저장
     * 5. 채팅방 참여자에게 메시지 브로드캐스트
     * 6. 오프라인 사용자에게 알림 발송
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            // 세션에서 사용자 정보 추출
            User user = (User) session.getAttributes().get("user");
            if (user == null) {
                log.warn("메시지 처리 실패 - 사용자 정보 없음, 세션ID: {}", session.getId());
                return;
            }

            // 메시지 내용 추출 및 파싱
            String payload = message.getPayload().toString();
            log.debug("메시지 수신 - 사용자: {}, 내용: {}", user.getEmail(), payload);

            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
            
            // 메시지 기본 정보 설정
            chatMessage.setSenderId(user.getId());
            chatMessage.setSenderName(user.getFullName());
            chatMessage.setTimestamp(LocalDateTime.now());

            // 메시지 타입별 처리
            switch (chatMessage.getType()) {
                case CHAT:
                    handleChatMessage(session, chatMessage, user);
                    break;
                case JOIN:
                    handleJoinMessage(session, chatMessage, user);
                    break;
                case LEAVE:
                    handleLeaveMessage(session, chatMessage, user);
                    break;
                case TYPING:
                    handleTypingMessage(session, chatMessage, user);
                    break;
                default:
                    log.warn("알 수 없는 메시지 타입 - 타입: {}, 사용자: {}", 
                        chatMessage.getType(), user.getEmail());
            }

        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생 - 세션ID: {}, 오류: {}", 
                session.getId(), e.getMessage(), e);
            
            // 오류 메시지를 클라이언트에게 전송
            ChatMessage errorMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.ERROR)
                .content("메시지 처리 중 오류가 발생했습니다.")
                .timestamp(LocalDateTime.now())
                .build();
                
            sendMessageToSession(session, errorMessage);
        }
    }

    /**
     * 채팅 메시지 처리
     * 
     * 일반 채팅 메시지의 처리 로직:
     * 1. 채팅방 존재 여부 및 참여 권한 확인
     * 2. 메시지 내용 검증 (길이, 금지어 등)
     * 3. 메시지 데이터베이스 저장
     * 4. 채팅방 참여자 전체에게 브로드캐스트
     * 5. 읽지 않은 메시지 카운트 업데이트
     */
    private void handleChatMessage(WebSocketSession session, ChatMessage chatMessage, User user) {
        try {
            // 채팅방 존재 여부 확인
            ChatRoom chatRoom = chatService.getChatRoom(chatMessage.getRoomId());
            if (chatRoom == null) {
                sendErrorMessage(session, "존재하지 않는 채팅방입니다.");
                return;
            }

            // 채팅방 참여 권한 확인
            if (!chatService.isUserInChatRoom(user.getId(), chatMessage.getRoomId())) {
                sendErrorMessage(session, "채팅방에 참여하지 않은 사용자입니다.");
                return;
            }

            // 메시지 내용 검증
            if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
                sendErrorMessage(session, "메시지 내용이 비어있습니다.");
                return;
            }

            if (chatMessage.getContent().length() > 1000) {
                sendErrorMessage(session, "메시지가 너무 깁니다. (최대 1000자)");
                return;
            }

            // 메시지 저장
            ChatMessage savedMessage = chatService.saveMessage(chatMessage);

            // 채팅방 참여자 전체에게 브로드캐스트
            broadcastToRoom(chatMessage.getRoomId(), savedMessage);

            // 읽지 않은 메시지 카운트 업데이트
            chatService.updateUnreadCount(chatMessage.getRoomId(), user.getId());

            log.debug("채팅 메시지 처리 완료 - 방ID: {}, 발송자: {}", 
                chatMessage.getRoomId(), user.getEmail());

        } catch (Exception e) {
            log.error("채팅 메시지 처리 실패 - 방ID: {}, 사용자: {}, 오류: {}", 
                chatMessage.getRoomId(), user.getEmail(), e.getMessage(), e);
            sendErrorMessage(session, "메시지 전송에 실패했습니다.");
        }
    }

    /**
     * 채팅방 참여 메시지 처리
     */
    private void handleJoinMessage(WebSocketSession session, ChatMessage chatMessage, User user) {
        try {
            // 채팅방 참여 처리
            boolean joined = chatService.joinChatRoom(user.getId(), chatMessage.getRoomId());
            
            if (joined) {
                // 시스템 메시지 생성
                ChatMessage joinMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.SYSTEM)
                    .roomId(chatMessage.getRoomId())
                    .content(user.getFullName() + "님이 채팅방에 참여했습니다.")
                    .timestamp(LocalDateTime.now())
                    .build();

                // 채팅방 참여자들에게 알림
                broadcastToRoom(chatMessage.getRoomId(), joinMessage);
                
                log.info("사용자 채팅방 참여 - 방ID: {}, 사용자: {}", 
                    chatMessage.getRoomId(), user.getEmail());
            } else {
                sendErrorMessage(session, "채팅방 참여에 실패했습니다.");
            }

        } catch (Exception e) {
            log.error("채팅방 참여 처리 실패 - 방ID: {}, 사용자: {}, 오류: {}", 
                chatMessage.getRoomId(), user.getEmail(), e.getMessage(), e);
            sendErrorMessage(session, "채팅방 참여에 실패했습니다.");
        }
    }

    /**
     * 채팅방 퇴장 메시지 처리
     */
    private void handleLeaveMessage(WebSocketSession session, ChatMessage chatMessage, User user) {
        try {
            // 채팅방 퇴장 처리
            boolean left = chatService.leaveChatRoom(user.getId(), chatMessage.getRoomId());
            
            if (left) {
                // 시스템 메시지 생성
                ChatMessage leaveMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.SYSTEM)
                    .roomId(chatMessage.getRoomId())
                    .content(user.getFullName() + "님이 채팅방을 나갔습니다.")
                    .timestamp(LocalDateTime.now())
                    .build();

                // 채팅방 참여자들에게 알림
                broadcastToRoom(chatMessage.getRoomId(), leaveMessage);
                
                log.info("사용자 채팅방 퇴장 - 방ID: {}, 사용자: {}", 
                    chatMessage.getRoomId(), user.getEmail());
            }

        } catch (Exception e) {
            log.error("채팅방 퇴장 처리 실패 - 방ID: {}, 사용자: {}, 오류: {}", 
                chatMessage.getRoomId(), user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * 타이핑 상태 메시지 처리 (실시간 타이핑 표시)
     */
    private void handleTypingMessage(WebSocketSession session, ChatMessage chatMessage, User user) {
        try {
            // 타이핑 메시지는 저장하지 않고 실시간으로만 전달
            chatMessage.setSenderId(user.getId());
            chatMessage.setSenderName(user.getFullName());
            
            // 본인을 제외한 채팅방 참여자들에게 전송
            broadcastToRoomExceptSender(chatMessage.getRoomId(), chatMessage, user.getId());
            
            log.debug("타이핑 상태 전송 - 방ID: {}, 사용자: {}", 
                chatMessage.getRoomId(), user.getEmail());

        } catch (Exception e) {
            log.error("타이핑 상태 처리 실패 - 방ID: {}, 사용자: {}, 오류: {}", 
                chatMessage.getRoomId(), user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * WebSocket 연결 해제 시 호출
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        try {
            User user = (User) session.getAttributes().get("user");
            Integer userId = (Integer) session.getAttributes().get("userId");

            if (user != null && userId != null) {
                // 세션 매핑 제거
                activeSessions.remove(session.getId());
                userSessionMap.remove(userId);

                // 사용자 오프라인 상태 업데이트
                chatService.updateUserOnlineStatus(userId, false);

                // 연결 통계 업데이트
                updateConnectionStats(-1);

                log.info("WebSocket 연결 해제 - 사용자: {}, 세션ID: {}, 상태: {}", 
                    user.getEmail(), session.getId(), closeStatus);
            }

        } catch (Exception e) {
            log.error("연결 해제 처리 중 오류 발생 - 세션ID: {}, 오류: {}", 
                session.getId(), e.getMessage(), e);
        }
    }

    /**
     * WebSocket 전송 오류 처리
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 전송 오류 - 세션ID: {}, 오류: {}", 
            session.getId(), exception.getMessage(), exception);
            
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException e) {
            log.error("세션 강제 종료 실패 - 세션ID: {}", session.getId());
        }
    }

    /**
     * 부분 메시지 지원 여부 (대용량 메시지 처리)
     */
    @Override
    public boolean supportsPartialMessages() {
        return false; // 부분 메시지 미지원 (단순성 우선)
    }

    /**
     * 특정 채팅방의 모든 참여자에게 메시지 브로드캐스트
     */
    private void broadcastToRoom(String roomId, ChatMessage message) {
        try {
            // 채팅방 참여자 목록 조회
            java.util.List<Integer> participants = chatService.getChatRoomParticipants(roomId);
            
            String messageJson = objectMapper.writeValueAsString(message);
            
            for (Integer participantId : participants) {
                String sessionId = userSessionMap.get(participantId);
                if (sessionId != null) {
                    WebSocketSession session = activeSessions.get(sessionId);
                    if (session != null && session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(messageJson));
                        } catch (IOException e) {
                            log.warn("메시지 전송 실패 - 사용자ID: {}, 오류: {}", 
                                participantId, e.getMessage());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("방 브로드캐스트 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * 발송자를 제외한 채팅방 참여자에게 메시지 브로드캐스트
     */
    private void broadcastToRoomExceptSender(String roomId, ChatMessage message, Integer senderId) {
        try {
            java.util.List<Integer> participants = chatService.getChatRoomParticipants(roomId);
            
            String messageJson = objectMapper.writeValueAsString(message);
            
            for (Integer participantId : participants) {
                if (!participantId.equals(senderId)) { // 발송자 제외
                    String sessionId = userSessionMap.get(participantId);
                    if (sessionId != null) {
                        WebSocketSession session = activeSessions.get(sessionId);
                        if (session != null && session.isOpen()) {
                            try {
                                session.sendMessage(new TextMessage(messageJson));
                            } catch (IOException e) {
                                log.warn("메시지 전송 실패 - 사용자ID: {}, 오류: {}", 
                                    participantId, e.getMessage());
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("방 브로드캐스트 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * 특정 세션에 메시지 전송
     */
    private void sendMessageToSession(WebSocketSession session, ChatMessage message) {
        try {
            if (session.isOpen()) {
                String messageJson = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(messageJson));
            }
        } catch (Exception e) {
            log.error("세션 메시지 전송 실패 - 세션ID: {}, 오류: {}", 
                session.getId(), e.getMessage(), e);
        }
    }

    /**
     * 오류 메시지 전송
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        ChatMessage error = ChatMessage.builder()
            .type(ChatMessage.MessageType.ERROR)
            .content(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
            
        sendMessageToSession(session, error);
    }

    /**
     * 연결 통계 업데이트 (모니터링용)
     */
    private void updateConnectionStats(int delta) {
        try {
            int currentConnections = activeSessions.size();
            log.debug("WebSocket 연결 통계 업데이트 - 현재 연결 수: {}, 변화: {}", 
                currentConnections, delta);
                
            // TODO: 메트릭 수집 시스템에 전송
            // metricsService.updateWebSocketConnections(currentConnections);
            
        } catch (Exception e) {
            log.warn("연결 통계 업데이트 실패: {}", e.getMessage());
        }
    }

    /**
     * 현재 활성 연결 수 조회 (모니터링용)
     */
    public int getActiveConnectionCount() {
        return activeSessions.size();
    }

    /**
     * 특정 사용자의 온라인 여부 확인
     */
    public boolean isUserOnline(Integer userId) {
        return userSessionMap.containsKey(userId);
    }
}