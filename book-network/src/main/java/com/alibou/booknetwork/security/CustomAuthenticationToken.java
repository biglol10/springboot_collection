package com.alibou.booknetwork.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 커스텀 인증 토큰
 * 
 * Spring Security의 AbstractAuthenticationToken을 확장한 커스텀 인증 토큰입니다.
 * SSGD 스타일의 JWT 기반 인증 방식을 Book-Network에 적용한 버전입니다.
 * 
 * 주요 특징:
 * - JWT 방식에 최적화 (credentials 불필요)
 * - UserDetails 또는 커스텀 Principal 지원
 * - 확장 가능한 구조
 */
public class CustomAuthenticationToken extends AbstractAuthenticationToken {
    
    private final Object principal;

    /**
     * 인증된 사용자를 위한 생성자
     * 
     * @param principal 사용자 정보 (UserDetails 구현체)
     * @param authorities 권한 목록
     */
    public CustomAuthenticationToken(Object principal, 
                                   Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true); // JWT 토큰 기반이므로 이미 인증된 상태로 설정
    }

    /**
     * 자격 증명을 반환합니다.
     * JWT 방식에서는 토큰 자체가 인증 수단이므로 별도의 자격 증명이 필요하지 않습니다.
     * 
     * @return null (JWT 방식에서는 자격 증명 저장 불필요)
     */
    @Override
    public Object getCredentials() {
        return null; // JWT 방식에서는 자격 증명 저장 불필요
    }

    /**
     * 인증된 사용자 정보를 반환합니다.
     * 
     * @return 사용자 정보 객체 (UserDetails 또는 커스텀 Principal)
     */
    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    /**
     * 인증 토큰의 상세 정보를 문자열로 반환합니다.
     * 디버깅 및 로깅 목적으로 사용됩니다.
     * 
     * @return 토큰 정보 문자열
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(" [");
        sb.append("Principal=").append(getPrincipal());
        sb.append(", Authorities=").append(getAuthorities());
        sb.append(", Authenticated=").append(isAuthenticated());
        sb.append("]");
        return sb.toString();
    }
}