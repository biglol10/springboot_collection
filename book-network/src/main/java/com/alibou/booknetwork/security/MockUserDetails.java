package com.alibou.booknetwork.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Mock 사용자 정보 클래스
 * 
 * 개발 환경에서 테스트용 사용자 인증을 위한 UserDetails 구현체입니다.
 * 실제 데이터베이스 조회 없이 임시 사용자 정보를 제공합니다.
 */
@Getter
@Builder
public class MockUserDetails implements UserDetails {
    
    private final Integer id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final Collection<? extends GrantedAuthority> authorities;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return ""; // Mock 사용자는 비밀번호 불필요
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * 사용자의 전체 이름을 반환합니다.
     * 
     * @return firstName + lastName 조합
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return email; // 이름이 없으면 이메일 반환
    }
}