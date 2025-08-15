package com.alibou.booknetwork.security;

import com.alibou.booknetwork.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 커스텀 인증 제공자
 * 
 * SSGD 스타일의 엔터프라이즈급 인증 제공자를 Book-Network에 적용한 버전입니다.
 * 사용자 정보와 권한을 기반으로 인증 객체를 생성하는 역할을 합니다.
 * 
 * 주요 기능:
 * - 사용자 정보 기반 Authentication 객체 생성
 * - 권한 정보 처리 및 변환
 * - 개발환경 Mock 로그인 지원
 * - 확장 가능한 인증 로직 제공
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider {

    /**
     * 사용자 정보와 권한 목록을 사용해 인증 객체를 생성하는 메서드
     * 
     * 이 메서드는 JWT 토큰에서 추출한 사용자 정보와 권한 목록을 기반으로
     * Spring Security의 Authentication 객체를 생성합니다.
     * 
     * @param userDetails 사용자 상세 정보 (UserDetails 구현체)
     * @param authorities 사용자의 권한 목록
     * @return 생성된 인증 객체
     */
    public Authentication getAuthentication(UserDetails userDetails, List<String> authorities) {
        Collection<? extends GrantedAuthority> grantedAuthorities = null;
        
        if (authorities != null && !authorities.isEmpty()) {
            grantedAuthorities = authorities.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            // 권한이 없는 경우 기본 사용자 권한 부여
            grantedAuthorities = userDetails.getAuthorities();
        }

        return new CustomAuthenticationToken(userDetails, grantedAuthorities);
    }

    /**
     * Mock 로그인을 위한 인증 객체 생성 메서드
     * 개발 환경에서 테스트용 사용자 정보로 인증 객체를 생성합니다.
     * 
     * @param username 사용자명 (이메일)
     * @param authorities 권한 목록
     * @param userId 사용자 ID (선택적)
     * @param roles 역할 목록 (선택적)
     * @return 생성된 인증 객체
     */
    public Authentication getMockAuthentication(
            String username, 
            String[] authorities, 
            Integer userId, 
            String[] roles) {

        Collection<? extends GrantedAuthority> grantedAuthorities;
        
        // 권한 처리 우선순위: authorities > roles > 기본 USER
        if (authorities != null && authorities.length > 0) {
            grantedAuthorities = Arrays.stream(authorities)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else if (roles != null && roles.length > 0) {
            grantedAuthorities = Arrays.stream(roles)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        } else {
            grantedAuthorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Mock 사용자 정보 생성
        MockUserDetails mockUser = MockUserDetails.builder()
                .id(userId)
                .email(username)
                .authorities(grantedAuthorities)
                .build();

        return new CustomAuthenticationToken(mockUser, grantedAuthorities);
    }

    /**
     * 향후 확장을 위한 메서드
     * 복잡한 비즈니스 로직이 필요한 경우 이 메서드를 사용할 수 있습니다.
     * 
     * @param userDetails 사용자 정보
     * @param additionalClaims 추가 클레임 정보
     * @return 생성된 인증 객체
     */
    public Authentication getEnhancedAuthentication(
            UserDetails userDetails, 
            java.util.Map<String, Object> additionalClaims) {
        
        // 추가 클레임에서 권한 정보 추출
        @SuppressWarnings("unchecked")
        List<String> authorities = (List<String>) additionalClaims.get("authorities");
        
        // 추가 비즈니스 로직을 여기에 구현할 수 있습니다.
        // 예: 사용자 그룹별 권한 처리, 동적 권한 부여 등
        
        return getAuthentication(userDetails, authorities);
    }

    /**
     * 특별한 권한 처리가 필요한 경우를 위한 메서드
     * SSGD의 "ALL" 권한과 같은 특수 케이스를 처리합니다.
     * 
     * @param userDetails 사용자 정보
     * @param authorityType 권한 타입 ("ALL", "SUPER_ADMIN", "SYSTEM_ADMIN", "BOOK_MANAGER", "USER" 등)
     * @return 생성된 인증 객체
     */
    public Authentication getSpecialAuthentication(UserDetails userDetails, String authorityType) {
        Collection<? extends GrantedAuthority> grantedAuthorities;

        switch (authorityType.toUpperCase()) {
            case "ALL":
                // 모든 권한 부여 (BookAuthorities의 모든 권한 사용)
                grantedAuthorities = Arrays.stream(BookAuthorities.getAllAuthorities())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                break;
            case "SUPER_ADMIN":
                grantedAuthorities = Arrays.stream(BookAuthorities.SUPER_ADMIN_AUTHORITIES)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                break;
            case "SYSTEM_ADMIN":
                grantedAuthorities = Arrays.stream(BookAuthorities.SYSTEM_ADMIN_AUTHORITIES)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                break;
            case "BOOK_MANAGER":
                grantedAuthorities = Arrays.stream(BookAuthorities.BOOK_MANAGER_AUTHORITIES)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                break;
            case "USER":
                grantedAuthorities = Arrays.stream(BookAuthorities.USER_BASIC_AUTHORITIES)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                break;
            default:
                grantedAuthorities = userDetails.getAuthorities();
                break;
        }

        return new CustomAuthenticationToken(userDetails, grantedAuthorities);
    }

    /**
     * 역할 기반 인증 객체 생성
     * 역할에 따라 미리 정의된 권한 그룹을 자동으로 할당합니다.
     * 
     * @param userDetails 사용자 정보
     * @param role 사용자 역할
     * @return 생성된 인증 객체
     */
    public Authentication getRoleBasedAuthentication(UserDetails userDetails, String role) {
        String[] authorities = BookAuthorities.getAuthoritiesByRole(role);
        Collection<? extends GrantedAuthority> grantedAuthorities = 
                Arrays.stream(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        return new CustomAuthenticationToken(userDetails, grantedAuthorities);
    }
}