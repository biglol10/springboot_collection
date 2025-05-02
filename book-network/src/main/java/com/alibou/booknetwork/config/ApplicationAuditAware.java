package com.alibou.booknetwork.config;

import com.alibou.booknetwork.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA 엔티티 감사 정보 제공자
 * 
 * 이 클래스는 Spring Data JPA의 감사(Auditing) 기능을 구현하여 엔티티의 생성자와 수정자 정보를
 * 자동으로 채울 수 있도록 해줍니다. JPA 엔티티에서 @CreatedBy, @LastModifiedBy 어노테이션이 
 * 지정된 필드에 현재 인증된 사용자의 ID를 자동으로 설정합니다.
 * 
 * 엔티티 클래스에 다음과 같이 필드를 정의하여 사용할 수 있습니다:
 * ```
 * @CreatedBy
 * @Column(updatable = false)
 * private Integer createdBy;
 * 
 * @LastModifiedBy
 * private Integer lastModifiedBy;
 * ```
 * 
 * 이 기능을 활성화하려면 @EnableJpaAuditing 어노테이션을 구성 클래스에 추가해야 합니다.
 */
@Component
public class ApplicationAuditAware implements AuditorAware<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationAuditAware.class);

    /**
     * 현재 감사자(로그인한 사용자) ID를 반환합니다.
     * 
     * 이 메소드는 다음 단계로 현재 인증된 사용자의 ID를 확인합니다:
     * 1. SecurityContextHolder에서 현재 인증 객체 획득
     * 2. 인증 객체의 유효성 검사 (null, 인증 상태, 익명 사용자 확인)
     * 3. User 객체 추출 및 ID 반환
     * 
     * 인증된 사용자가 없거나 익명 사용자인 경우 비어있는 Optional을 반환합니다.
     * 
     * @return 현재 인증된 사용자의 ID를 포함한 Optional, 또는 인증된 사용자가 없는 경우 empty Optional
     */
    @Override
    public Optional<Integer> getCurrentAuditor() {
        try {
            // SecurityContextHolder에서 현재 인증 객체 획득
            // JWT 필터 등의 인증 과정에서 SecurityContext에 Authentication 객체가 설정됨
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
            // 인증되지 않았거나 익명 사용자인 경우 비어있는 Optional 반환
            if (authentication == null || 
                !authentication.isAuthenticated() || 
                authentication instanceof AnonymousAuthenticationToken) {
                logger.debug("감사 정보 기록: 인증된 사용자 없음");
                return Optional.empty();
            }
    
            // 인증 객체로부터 User 객체 추출
            Object principal = authentication.getPrincipal();
            
            // Principal이 User 타입인지 검증
            if (!(principal instanceof User)) {
                logger.warn("감사 정보 기록: Principal이 예상된 User 타입이 아님 - {}", 
                        principal != null ? principal.getClass().getName() : "null");
                return Optional.empty();
            }
    
            // Principal에서 User ID 추출
            User userPrincipal = (User) principal;
            Integer userId = userPrincipal.getId();
            
            logger.debug("감사 정보 기록: 사용자 ID {}", userId);
            return Optional.ofNullable(userId);
            
        } catch (Exception e) {
            // 예외 발생 시 로깅하고 빈 결과 반환
            logger.error("감사자 ID 조회 중 오류 발생", e);
            return Optional.empty();
        }
    }
    
    /**
     * 시스템 작업용 사용자 ID를 반환합니다.
     * 
     * 배치 작업이나 시스템 프로세스와 같이 인증된 사용자 컨텍스트 없이
     * 실행되는 작업에서 사용할 수 있는 시스템 사용자 ID입니다.
     * 
     * @return 시스템 사용자 ID
     */
    @Nullable
    public Integer getSystemAuditor() {
        return -1; // 시스템 사용자를 나타내는 특별한 ID
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 보다 풍부한 감사 정보 저장:
 *    - 단순 ID 대신 복합 감사 정보 제공
 *    - 사용자명, IP 주소, 세션 ID 등을 포함한 감사 객체 설계
 *    
 *    예시:
 *    @Component
 *    public class EnhancedAuditingHandler implements AuditorAware<AuditInfo> {
 *        @Override
 *        public Optional<AuditInfo> getCurrentAuditor() {
 *            // 현재 인증 정보 및 요청 정보를 조합하여 AuditInfo 객체 생성
 *            return Optional.of(new AuditInfo(userId, username, ipAddress, sessionId));
 *        }
 *    }
 * 
 * 2. 다양한 컨텍스트에서의 감사 지원:
 *    - 웹 요청, 배치 작업, 메시지 처리 등 다양한 컨텍스트 지원
 *    - ThreadLocal 기반 컨텍스트 전파 메커니즘 구현
 *    
 *    예시:
 *    public class AuditContextHolder {
 *        private static final ThreadLocal<AuditContext> contextHolder = new ThreadLocal<>();
 *        
 *        public static void setContext(AuditContext context) {
 *            contextHolder.set(context);
 *        }
 *        
 *        public static AuditContext getContext() {
 *            return contextHolder.get();
 *        }
 *        
 *        public static void clear() {
 *            contextHolder.remove();
 *        }
 *    }
 * 
 * 3. 위임 감사(Delegated Auditing) 지원:
 *    - 다른 사용자를 대신하여 작업할 때의 감사 정보 추적
 *    - 원래 사용자와 실제 작업 수행자 모두 기록
 *    
 *    예시:
 *    @RequiredArgsConstructor
 *    public class DelegatedAuditAware implements AuditorAware<DelegatedAuditInfo> {
 *        private final HttpServletRequest request;
 *        
 *        @Override
 *        public Optional<DelegatedAuditInfo> getCurrentAuditor() {
 *            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *            String impersonator = request.getHeader("X-Impersonator-Id");
 *            
 *            if (auth != null && impersonator != null) {
 *                return Optional.of(new DelegatedAuditInfo(
 *                    ((User) auth.getPrincipal()).getId(), 
 *                    Integer.valueOf(impersonator))
 *                );
 *            }
 *            // 일반 감사 정보 반환
 *        }
 *    }
 * 
 * 4. 트랜잭션 메타데이터 통합:
 *    - 트랜잭션 ID, 작업 유형, 근거 등 메타데이터 통합
 *    - 변경 사유 등 추가 정보 기록
 *    
 *    예시:
 *    @Data
 *    public class TransactionMetadata {
 *        private final String transactionId;
 *        private final String operationType;
 *        private final String reason;
 *        private final Integer userId;
 *    }
 *    
 *    // TransactionMetadata를 ThreadLocal에 저장하고 엔티티 생성/수정 시 접근
 * 
 * 5. 역할 기반 감사:
 *    - 사용자 ID와 함께 역할 정보 함께 저장
 *    - 권한 변경 추적을 위한 이력 관리
 * 
 * 6. 추적성 향상을 위한 감사 이벤트:
 *    - 엔티티 변경 시 감사 이벤트 발행
 *    - 별도의 감사 저장소에 상세 변경 내역 기록
 * 
 * 7. 성능 최적화:
 *    - 캐싱을 통한 빈번한 감사 정보 조회 성능 개선
 *    - 대량 작업에서의 감사 정보 배치 처리
 * 
 * 8. 테스트 지원:
 *    - 테스트 환경에서의 감사 정보 모의 구현
 *    - 테스트 사용자 컨텍스트 설정 헬퍼 메소드
 */
