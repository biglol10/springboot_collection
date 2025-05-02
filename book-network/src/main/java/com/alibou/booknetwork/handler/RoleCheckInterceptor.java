package com.alibou.booknetwork.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collection;

/**
 * 역할 기반 접근 제어(RBAC) 인터셉터
 * 
 * 이 인터셉터는 특정 역할을 가진 사용자만 특정 API에 접근할 수 있도록 제한합니다.
 * Spring MVC의 HandlerInterceptor를 구현하여 컨트롤러 메소드 실행 전에 사용자의 권한을 검사합니다.
 * 주요 사용 사례:
 * - 특정 역할(예: ADMIN)을 가진 사용자만 특정 API를 호출할 수 있도록 제한
 * - 사용자가 자신이 소유한 리소스만 수정하거나 삭제할 수 있도록 제한
 */
@Component
public class RoleCheckInterceptor implements HandlerInterceptor {

    /**
     * 컨트롤러 메소드 실행 전에 호출되는 메소드
     * 사용자의 인증 상태와 권한을 검사하여 접근 가능 여부를 결정합니다.
     * 
     * 처리 과정:
     * 1. SecurityContext에서 현재 인증 객체 가져오기
     * 2. 인증 상태 확인
     * 3. 사용자의 권한 중 'ROLE_ADMIN' 권한이 있는지 확인
     * 4. 권한이 없으면 403 Forbidden 응답 반환
     * 
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param handler 실행될 핸들러 (컨트롤러 메소드)
     * @return true이면 핸들러 실행을 계속 진행, false이면 핸들러 실행을 중단
     * @throws Exception 처리 중 발생할 수 있는 예외
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // SecurityContext에서 현재 사용자의 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 객체가 없거나 인증되지 않은 경우 401 Unauthorized 응답 반환
        if (authentication == null || !authentication.isAuthenticated()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return false; // 요청 처리 중단
        }

        // 사용자의 권한 목록 가져오기
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        // 'ROLE_ADMIN' 권한이 있는지 확인
        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 관리자 권한이 없는 경우 403 Forbidden 응답 반환
        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return false; // 요청 처리 중단
        }

        // 모든 검사를 통과하면 요청 처리 계속 진행
        return true;
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 동적 역할 확인:
 *    - 하드코딩된 'ROLE_ADMIN' 대신 설정에서 역할 목록을 주입
 *    - 경로별로 다른 역할 요구사항 적용
 *    
 *    private final Set<String> requiredRoles;
 *    
 *    public RoleCheckInterceptor(@Value("${security.required-roles}") String[] roles) {
 *        this.requiredRoles = new HashSet<>(Arrays.asList(roles));
 *    }
 * 
 * 2. 메소드 수준 권한 확인:
 *    - @PreAuthorize, @PostAuthorize 어노테이션 활용
 *    - SpEL(Spring Expression Language)을 사용한 복잡한 조건 표현
 *    
 *    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #userId == authentication.principal.id)")
 *    public User updateUser(@PathVariable Long userId, @RequestBody User user) { ... }
 * 
 * 3. 커스텀 어노테이션 기반 접근 제어:
 *    - 인터셉터와 함께 사용할 커스텀 어노테이션 정의
 *    - 컨트롤러 메소드에 적용하여 세밀한 제어 구현
 *    
 *    @Target({ElementType.METHOD})
 *    @Retention(RetentionPolicy.RUNTIME)
 *    public @interface RequireRole {
 *        String[] value();
 *    }
 * 
 * 4. 리소스 소유권 확인:
 *    - 요청 데이터와 인증된 사용자 정보를 비교
 *    - 데이터베이스 조회를 통한 소유권 검증
 *    
 *    Long requestedBookId = Long.parseLong(request.getParameter("bookId"));
 *    Long currentUserId = ((UserDetails) authentication.getPrincipal()).getId();
 *    boolean isOwner = bookRepository.existsByIdAndAuthorId(requestedBookId, currentUserId);
 * 
 * 5. 접근 제어 결정 캐싱:
 *    - 자주 수행되는 권한 검사 결과 캐싱
 *    - 성능 향상을 위한 전략
 * 
 * 6. IP 기반 추가 보안:
 *    - 특정 역할의 접근을 특정 IP 범위로 제한
 *    - 지리적 위치 기반 접근 제어 추가
 * 
 * 7. 감사 로깅:
 *    - 접근 거부 이벤트에 대한 상세 로깅
 *    - 보안 위반 시도 감지 및 알림
 * 
 * 8. 중앙 집중식 보안 정책:
 *    - 외부 정책 저장소와 통합
 *    - 런타임에 정책 업데이트 지원
 */
