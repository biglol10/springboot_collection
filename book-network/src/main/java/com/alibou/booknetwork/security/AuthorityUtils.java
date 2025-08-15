package com.alibou.booknetwork.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 권한 검사 유틸리티 클래스
 * 
 * SSGD의 AttributeAuthorityUtility를 참고하여 Book Network에 맞게 구현한 권한 검사 도구입니다.
 * 복잡한 권한 조건을 쉽게 검사할 수 있는 정적 메서드들을 제공합니다.
 */
public class AuthorityUtils {

    /**
     * 현재 인증된 사용자가 특정 권한을 가지고 있는지 확인합니다.
     * 
     * @param authority 확인할 권한
     * @return 권한이 있으면 true, 없으면 false
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    /**
     * 현재 인증된 사용자가 여러 권한 중 하나라도 가지고 있는지 확인합니다.
     * 
     * @param authorities 확인할 권한들
     * @return 권한 중 하나라도 있으면 true, 모두 없으면 false
     */
    public static boolean hasAnyAuthority(String... authorities) {
        if (authorities == null || authorities.length == 0) {
            return false;
        }

        return Arrays.stream(authorities)
                .anyMatch(AuthorityUtils::hasAuthority);
    }

    /**
     * 현재 인증된 사용자가 모든 권한을 가지고 있는지 확인합니다.
     * 
     * @param authorities 확인할 권한들
     * @return 모든 권한이 있으면 true, 하나라도 없으면 false
     */
    public static boolean hasAllAuthorities(String... authorities) {
        if (authorities == null || authorities.length == 0) {
            return true;
        }

        return Arrays.stream(authorities)
                .allMatch(AuthorityUtils::hasAuthority);
    }

    /**
     * 현재 인증된 사용자가 특정 역할을 가지고 있는지 확인합니다.
     * 
     * @param role 확인할 역할 (ROLE_ 접두사 자동 추가)
     * @return 역할이 있으면 true, 없으면 false
     */
    public static boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return hasAuthority(roleWithPrefix);
    }

    /**
     * 현재 인증된 사용자가 여러 역할 중 하나라도 가지고 있는지 확인합니다.
     * 
     * @param roles 확인할 역할들
     * @return 역할 중 하나라도 있으면 true, 모두 없으면 false
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }

        return Arrays.stream(roles)
                .anyMatch(AuthorityUtils::hasRole);
    }

    /**
     * 현재 인증된 사용자의 모든 권한을 반환합니다.
     * 
     * @return 권한 문자열 집합
     */
    public static Set<String> getCurrentUserAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * 현재 인증된 사용자 정보를 반환합니다.
     * 
     * @return UserDetails 객체 또는 null
     */
    public static UserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return (UserDetails) principal;
        }

        return null;
    }

    /**
     * 현재 인증된 사용자명을 반환합니다.
     * 
     * @return 사용자명 또는 null
     */
    public static String getCurrentUsername() {
        UserDetails userDetails = getCurrentUser();
        return userDetails != null ? userDetails.getUsername() : null;
    }

    /**
     * 현재 사용자가 도서 관련 권한을 가지고 있는지 확인합니다.
     * 
     * @return 도서 관련 권한이 있으면 true
     */
    public static boolean canAccessBooks() {
        return hasAnyAuthority(
            BookAuthorities.BOOK_READ,
            BookAuthorities.BOOK_CUD,
            BookAuthorities.BOOK_CRUD,
            BookAuthorities.ADMIN_ALL
        );
    }

    /**
     * 현재 사용자가 도서를 수정할 수 있는지 확인합니다.
     * 
     * @return 도서 수정 권한이 있으면 true
     */
    public static boolean canModifyBooks() {
        return hasAnyAuthority(
            BookAuthorities.BOOK_CUD,
            BookAuthorities.BOOK_CRUD,
            BookAuthorities.ADMIN_ALL
        );
    }

    /**
     * 현재 사용자가 사용자 관리 권한을 가지고 있는지 확인합니다.
     * 
     * @return 사용자 관리 권한이 있으면 true
     */
    public static boolean canManageUsers() {
        return hasAnyAuthority(
            BookAuthorities.USER_MANAGE,
            BookAuthorities.ADMIN_ADVANCED,
            BookAuthorities.ADMIN_SUPER,
            BookAuthorities.ADMIN_ALL
        );
    }

    /**
     * 현재 사용자가 시스템 관리 권한을 가지고 있는지 확인합니다.
     * 
     * @return 시스템 관리 권한이 있으면 true
     */
    public static boolean canAccessSystem() {
        return hasAnyAuthority(
            BookAuthorities.SYSTEM_READ,
            BookAuthorities.SYSTEM_MANAGE,
            BookAuthorities.ADMIN_ADVANCED,
            BookAuthorities.ADMIN_SUPER,
            BookAuthorities.ADMIN_ALL
        );
    }

    /**
     * 현재 사용자가 관리자인지 확인합니다.
     * 
     * @return 관리자 권한이 있으면 true
     */
    public static boolean isAdmin() {
        return hasAnyAuthority(
            BookAuthorities.ADMIN_BASIC,
            BookAuthorities.ADMIN_ADVANCED,
            BookAuthorities.ADMIN_SUPER,
            BookAuthorities.ADMIN_ALL
        ) || hasAnyRole("ADMIN");
    }

    /**
     * 현재 사용자가 최고 관리자인지 확인합니다.
     * 
     * @return 최고 관리자 권한이 있으면 true
     */
    public static boolean isSuperAdmin() {
        return hasAnyAuthority(
            BookAuthorities.ADMIN_SUPER,
            BookAuthorities.ADMIN_ALL
        );
    }

    /**
     * SSGD 스타일의 복합 권한 검사
     * 여러 조건을 모두 만족하는지 확인합니다.
     * 
     * @param requiredAuthorities 필수 권한들
     * @param optionalAuthorities 선택 권한들 (하나라도 있으면 됨)
     * @return 모든 조건을 만족하면 true
     */
    public static boolean checkComplexAuthority(String[] requiredAuthorities, String[] optionalAuthorities) {
        // 필수 권한 모두 체크
        boolean hasRequiredAuth = requiredAuthorities == null || hasAllAuthorities(requiredAuthorities);
        
        // 선택 권한 하나라도 체크
        boolean hasOptionalAuth = optionalAuthorities == null || hasAnyAuthority(optionalAuthorities);
        
        return hasRequiredAuth && hasOptionalAuth;
    }

    /**
     * 권한 디버깅을 위한 메서드
     * 현재 사용자의 모든 권한 정보를 출력합니다.
     * 
     * @return 권한 정보 문자열
     */
    public static String debugCurrentUserAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "No authentication found";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("User: ").append(authentication.getName()).append("\n");
        sb.append("Authenticated: ").append(authentication.isAuthenticated()).append("\n");
        sb.append("Auth Type: ").append(authentication.getClass().getSimpleName()).append("\n");
        sb.append("Authorities: ");
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities.isEmpty()) {
            sb.append("None");
        } else {
            sb.append(authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", ")));
        }
        
        return sb.toString();
    }
}