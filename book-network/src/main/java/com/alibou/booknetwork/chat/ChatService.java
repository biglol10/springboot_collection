package com.alibou.booknetwork.chat;

import com.alibou.booknetwork.user.User;
import com.alibou.booknetwork.user.UserRepository;
import com.alibou.booknetwork.service.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅 서비스 클래스
 * 
 * 엔터프라이즈 채팅 서비스의 핵심 기능:
 * 1. 채팅방 생명주기 관리: 생성, 수정, 삭제, 아카이브
 * 2. 참여자 관리: 초대, 퇴장, 권한 변경, 음소거
 * 3. 메시지 관리: 전송, 수정, 삭제, 검색
 * 4. 실시간 상태 관리: 온라인 상태, 타이핑 상태
 * 5. 알림 관리: 읽음 상태, 알림 설정
 * 
 * 성능 최적화 전략:
 * - Redis 캐싱: 활성 채팅방 및 참여자 정보
 * - 읽기 전용 트랜잭션: 조회 성능 향상
 * - 배치 처리: 대량 메시지 및 참여자 처리
 * - 비동기 처리: 알림 발송, 통계 업데이트
 * 
 * SSGD 채팅 서비스 패턴:
 * - 도메인 주도 설계: 비즈니스 로직 중심 설계
 * - 이벤트 기반 아키텍처: 메시지 발송 시 이벤트 발행
 * - 캐시 우선 전략: 빈번한 조회 데이터 캐싱
 * - 감사 로깅: 모든 채팅 활동 추적
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final RedisCacheService redisCacheService;

    // 캐시 키 상수
    private static final String CHAT_ROOM_CACHE_PREFIX = "chat:room:";
    private static final String PARTICIPANTS_CACHE_PREFIX = "chat:participants:";
    private static final String ONLINE_USERS_KEY = "chat:online_users";

    /**
     * 채팅방 생성
     * 
     * 채팅방 생성 프로세스:
     * 1. 입력 데이터 검증
     * 2. 채팅방 엔티티 생성 및 저장
     * 3. 생성자를 첫 번째 참여자로 추가
     * 4. 캐시 업데이트
     * 5. 생성 이벤트 발행
     */
    @Transactional
    public ChatRoom createChatRoom(CreateChatRoomRequest request, Integer creatorId) {
        try {
            log.info("채팅방 생성 시작 - 생성자: {}, 이름: {}", creatorId, request.getName());

            // 생성자 조회
            User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + creatorId));

            // 채팅방 생성
            ChatRoom chatRoom = ChatRoom.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .roomType(request.getRoomType())
                .creator(creator)
                .maxParticipants(request.getMaxParticipants())
                .isPublic(request.getIsPublic())
                .preserveHistory(request.getPreserveHistory())
                .tags(request.getTags())
                .build();

            chatRoom = chatRoomRepository.save(chatRoom);

            // 생성자를 첫 번째 참여자로 추가
            ChatRoomParticipant creatorParticipant = ChatRoomParticipant.builder()
                .chatRoom(chatRoom)
                .user(creator)
                .role(ChatRoomParticipant.ParticipantRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();

            participantRepository.save(creatorParticipant);

            // 참여자 수 업데이트
            chatRoom.incrementParticipants();
            chatRoomRepository.save(chatRoom);

            // 캐시 업데이트
            cacheRoom(chatRoom);
            invalidateParticipantsCache(chatRoom.getId());

            log.info("채팅방 생성 완료 - ID: {}, 이름: {}", chatRoom.getId(), chatRoom.getName());
            
            // TODO: 채팅방 생성 이벤트 발행
            // eventPublisher.publishEvent(new ChatRoomCreatedEvent(chatRoom));

            return chatRoom;

        } catch (Exception e) {
            log.error("채팅방 생성 실패 - 생성자: {}, 오류: {}", creatorId, e.getMessage(), e);
            throw new RuntimeException("채팅방 생성에 실패했습니다.", e);
        }
    }

    /**
     * 채팅방 조회 (캐시 우선)
     */
    public ChatRoom getChatRoom(String roomId) {
        try {
            // 캐시에서 먼저 조회
            Optional<ChatRoom> cachedRoom = getCachedRoom(roomId);
            if (cachedRoom.isPresent()) {
                log.debug("채팅방 캐시 히트 - ID: {}", roomId);
                return cachedRoom.get();
            }

            // 데이터베이스에서 조회
            Optional<ChatRoom> room = chatRoomRepository.findByIdAndIsActiveTrue(roomId);
            if (room.isPresent()) {
                // 캐시에 저장
                cacheRoom(room.get());
                log.debug("채팅방 데이터베이스 조회 - ID: {}", roomId);
                return room.get();
            }

            log.warn("채팅방을 찾을 수 없음 - ID: {}", roomId);
            return null;

        } catch (Exception e) {
            log.error("채팅방 조회 실패 - ID: {}, 오류: {}", roomId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 사용자 참여 채팅방 목록 조회
     */
    public List<ChatRoom> getUserChatRooms(Integer userId) {
        try {
            log.debug("사용자 채팅방 목록 조회 - 사용자ID: {}", userId);
            
            return chatRoomRepository.findByUserIdAndActiveOrderByLastMessageAtDesc(userId);

        } catch (Exception e) {
            log.error("사용자 채팅방 목록 조회 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 채팅방 참여
     */
    @Transactional
    public boolean joinChatRoom(Integer userId, String roomId) {
        try {
            log.info("채팅방 참여 시작 - 사용자ID: {}, 방ID: {}", userId, roomId);

            // 채팅방 존재 여부 확인
            ChatRoom chatRoom = getChatRoom(roomId);
            if (chatRoom == null) {
                log.warn("존재하지 않는 채팅방 - 방ID: {}", roomId);
                return false;
            }

            // 사용자 조회
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // 이미 참여 중인지 확인
            Optional<ChatRoomParticipant> existingParticipant = 
                participantRepository.findBychatRoomIdAndUserId(roomId, userId);

            if (existingParticipant.isPresent()) {
                ChatRoomParticipant participant = existingParticipant.get();
                if (Boolean.TRUE.equals(participant.getIsActive())) {
                    log.info("이미 참여 중인 사용자 - 사용자ID: {}, 방ID: {}", userId, roomId);
                    return true;
                }
                
                // 재참여 처리
                participant.rejoinChatRoom();
                participantRepository.save(participant);
            } else {
                // 참여자 수 제한 확인
                if (!chatRoom.canAddParticipant()) {
                    log.warn("채팅방 참여자 수 초과 - 방ID: {}, 현재: {}, 최대: {}", 
                        roomId, chatRoom.getCurrentParticipants(), chatRoom.getMaxParticipants());
                    return false;
                }

                // 새 참여자 추가
                ChatRoomParticipant newParticipant = ChatRoomParticipant.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .role(ChatRoomParticipant.ParticipantRole.MEMBER)
                    .joinedAt(LocalDateTime.now())
                    .lastActivityAt(LocalDateTime.now())
                    .build();

                participantRepository.save(newParticipant);
            }

            // 참여자 수 업데이트
            chatRoom.incrementParticipants();
            chatRoomRepository.save(chatRoom);

            // 캐시 무효화
            invalidateRoomCache(roomId);
            invalidateParticipantsCache(roomId);

            log.info("채팅방 참여 완료 - 사용자ID: {}, 방ID: {}", userId, roomId);
            
            // TODO: 참여 이벤트 발행
            // eventPublisher.publishEvent(new UserJoinedChatRoomEvent(roomId, userId));

            return true;

        } catch (Exception e) {
            log.error("채팅방 참여 실패 - 사용자ID: {}, 방ID: {}, 오류: {}", userId, roomId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 채팅방 퇴장
     */
    @Transactional
    public boolean leaveChatRoom(Integer userId, String roomId) {
        try {
            log.info("채팅방 퇴장 시작 - 사용자ID: {}, 방ID: {}", userId, roomId);

            // 참여자 조회
            Optional<ChatRoomParticipant> participantOpt = 
                participantRepository.findBychatRoomIdAndUserIdAndIsActiveTrue(roomId, userId);

            if (participantOpt.isEmpty()) {
                log.warn("참여하지 않은 채팅방 퇴장 시도 - 사용자ID: {}, 방ID: {}", userId, roomId);
                return false;
            }

            ChatRoomParticipant participant = participantOpt.get();
            ChatRoom chatRoom = participant.getChatRoom();

            // 퇴장 처리
            participant.leaveChatRoom();
            participantRepository.save(participant);

            // 참여자 수 감소
            chatRoom.decrementParticipants();
            chatRoomRepository.save(chatRoom);

            // 캐시 무효화
            invalidateRoomCache(roomId);
            invalidateParticipantsCache(roomId);

            log.info("채팅방 퇴장 완료 - 사용자ID: {}, 방ID: {}", userId, roomId);

            // TODO: 퇴장 이벤트 발행
            // eventPublisher.publishEvent(new UserLeftChatRoomEvent(roomId, userId));

            return true;

        } catch (Exception e) {
            log.error("채팅방 퇴장 실패 - 사용자ID: {}, 방ID: {}, 오류: {}", userId, roomId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 메시지 저장
     */
    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        try {
            log.debug("메시지 저장 시작 - 방ID: {}, 발송자: {}", message.getRoomId(), message.getSenderId());

            // 메시지 저장 (타이핑 메시지는 제외)
            ChatMessage savedMessage = null;
            if (message.getType() != ChatMessage.MessageType.TYPING) {
                savedMessage = chatMessageRepository.save(message);

                // 채팅방 마지막 메시지 시간 및 메시지 수 업데이트
                updateChatRoomActivity(message.getRoomId());

                // 읽지 않은 메시지 수 업데이트
                updateUnreadCountForParticipants(message.getRoomId(), message.getSenderId());
            } else {
                savedMessage = message; // 타이핑 메시지는 저장하지 않고 그대로 반환
            }

            log.debug("메시지 저장 완료 - ID: {}", savedMessage.getId());
            return savedMessage;

        } catch (Exception e) {
            log.error("메시지 저장 실패 - 방ID: {}, 오류: {}", message.getRoomId(), e.getMessage(), e);
            throw new RuntimeException("메시지 저장에 실패했습니다.", e);
        }
    }

    /**
     * 채팅방 메시지 조회 (페이징)
     */
    public Page<ChatMessage> getChatMessages(String roomId, Pageable pageable) {
        try {
            log.debug("채팅방 메시지 조회 - 방ID: {}, 페이지: {}", roomId, pageable.getPageNumber());
            
            return chatMessageRepository.findByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId, pageable);

        } catch (Exception e) {
            log.error("채팅방 메시지 조회 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage(), e);
            return Page.empty();
        }
    }

    /**
     * 채팅방 참여자 목록 조회
     */
    public List<Integer> getChatRoomParticipants(String roomId) {
        try {
            // 캐시에서 먼저 조회
            List<Integer> cachedParticipants = getCachedParticipants(roomId);
            if (cachedParticipants != null) {
                log.debug("참여자 목록 캐시 히트 - 방ID: {}", roomId);
                return cachedParticipants;
            }

            // 데이터베이스에서 조회
            List<Integer> participants = participantRepository.findActiveUserIdsByRoomId(roomId);
            
            // 캐시에 저장
            cacheParticipants(roomId, participants);
            
            log.debug("참여자 목록 조회 완료 - 방ID: {}, 참여자 수: {}", roomId, participants.size());
            return participants;

        } catch (Exception e) {
            log.error("참여자 목록 조회 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 사용자가 채팅방에 참여 중인지 확인
     */
    public boolean isUserInChatRoom(Integer userId, String roomId) {
        try {
            List<Integer> participants = getChatRoomParticipants(roomId);
            return participants.contains(userId);

        } catch (Exception e) {
            log.error("채팅방 참여 확인 실패 - 사용자ID: {}, 방ID: {}, 오류: {}", userId, roomId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 사용자 온라인 상태 업데이트
     */
    public void updateUserOnlineStatus(Integer userId, boolean isOnline) {
        try {
            if (isOnline) {
                redisCacheService.cacheUserActivity(userId, "ONLINE", 
                    java.util.Map.of("timestamp", LocalDateTime.now().toString()));
            } else {
                redisCacheService.cacheUserActivity(userId, "OFFLINE", 
                    java.util.Map.of("timestamp", LocalDateTime.now().toString()));
            }

            log.debug("사용자 온라인 상태 업데이트 - 사용자ID: {}, 상태: {}", userId, isOnline ? "온라인" : "오프라인");

        } catch (Exception e) {
            log.error("온라인 상태 업데이트 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 읽지 않은 메시지 수 업데이트
     */
    @Transactional
    public void updateUnreadCount(String roomId, Integer senderId) {
        try {
            // 발송자를 제외한 모든 참여자의 읽지 않은 메시지 수 증가
            List<ChatRoomParticipant> participants = 
                participantRepository.findByRoomIdAndIsActiveTrueAndUserIdNot(roomId, senderId);

            for (ChatRoomParticipant participant : participants) {
                participant.incrementUnreadCount();
            }

            participantRepository.saveAll(participants);
            
            log.debug("읽지 않은 메시지 수 업데이트 완료 - 방ID: {}, 대상: {}명", roomId, participants.size());

        } catch (Exception e) {
            log.error("읽지 않은 메시지 수 업데이트 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * 채팅방 활동 업데이트 (마지막 메시지 시간, 메시지 수)
     */
    @Transactional
    public void updateChatRoomActivity(String roomId) {
        try {
            Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                ChatRoom room = roomOpt.get();
                room.incrementMessageCount();
                chatRoomRepository.save(room);

                // 캐시 무효화
                invalidateRoomCache(roomId);
            }

        } catch (Exception e) {
            log.error("채팅방 활동 업데이트 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * 읽지 않은 메시지 수 업데이트 (발송자 제외)
     */
    @Transactional
    public void updateUnreadCountForParticipants(String roomId, Integer senderId) {
        try {
            participantRepository.incrementUnreadCountExceptSender(roomId, senderId);
            log.debug("참여자 읽지 않은 메시지 수 업데이트 - 방ID: {}, 발송자 제외: {}", roomId, senderId);

        } catch (Exception e) {
            log.error("참여자 읽지 않은 메시지 수 업데이트 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * 캐시 관련 메서드들
     */
    private void cacheRoom(ChatRoom room) {
        try {
            // 간단한 문자열 캐시 구현 (실제로는 JSON 직렬화 사용)
            String roomInfo = String.format("%s:%s:%s:%d", 
                room.getName(), room.getRoomType(), room.getIsActive(), room.getCurrentParticipants());
            // redisCacheService에 저장하는 로직 구현 필요
            log.debug("채팅방 캐시 저장 - ID: {}", room.getId());

        } catch (Exception e) {
            log.warn("채팅방 캐시 저장 실패 - ID: {}, 오류: {}", room.getId(), e.getMessage());
        }
    }

    private Optional<ChatRoom> getCachedRoom(String roomId) {
        try {
            // 캐시에서 채팅방 정보 조회 (스텁 구현)
            // 실제로는 Redis에서 JSON을 ChatRoom 객체로 역직렬화
            return Optional.empty();

        } catch (Exception e) {
            log.warn("채팅방 캐시 조회 실패 - ID: {}, 오류: {}", roomId, e.getMessage());
            return Optional.empty();
        }
    }

    private void cacheParticipants(String roomId, List<Integer> participants) {
        try {
            // 참여자 목록 캐시 저장 (스텁 구현)
            log.debug("참여자 목록 캐시 저장 - 방ID: {}, 참여자 수: {}", roomId, participants.size());

        } catch (Exception e) {
            log.warn("참여자 목록 캐시 저장 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage());
        }
    }

    private List<Integer> getCachedParticipants(String roomId) {
        try {
            // 캐시에서 참여자 목록 조회 (스텁 구현)
            return null;

        } catch (Exception e) {
            log.warn("참여자 목록 캐시 조회 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage());
            return null;
        }
    }

    private void invalidateRoomCache(String roomId) {
        try {
            // 채팅방 캐시 무효화 (스텁 구현)
            log.debug("채팅방 캐시 무효화 - ID: {}", roomId);

        } catch (Exception e) {
            log.warn("채팅방 캐시 무효화 실패 - ID: {}, 오류: {}", roomId, e.getMessage());
        }
    }

    private void invalidateParticipantsCache(String roomId) {
        try {
            // 참여자 목록 캐시 무효화 (스텁 구현)
            log.debug("참여자 목록 캐시 무효화 - 방ID: {}", roomId);

        } catch (Exception e) {
            log.warn("참여자 목록 캐시 무효화 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage());
        }
    }

    /**
     * 채팅방 생성 요청 DTO
     */
    public static class CreateChatRoomRequest {
        private String name;
        private String description;
        private ChatRoom.RoomType roomType;
        private Integer maxParticipants;
        private Boolean isPublic;
        private Boolean preserveHistory;
        private String tags;

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public ChatRoom.RoomType getRoomType() { return roomType; }
        public Integer getMaxParticipants() { return maxParticipants; }
        public Boolean getIsPublic() { return isPublic; }
        public Boolean getPreserveHistory() { return preserveHistory; }
        public String getTags() { return tags; }

        // Setters
        public void setName(String name) { this.name = name; }
        public void setDescription(String description) { this.description = description; }
        public void setRoomType(ChatRoom.RoomType roomType) { this.roomType = roomType; }
        public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
        public void setPreserveHistory(Boolean preserveHistory) { this.preserveHistory = preserveHistory; }
        public void setTags(String tags) { this.tags = tags; }
    }
}