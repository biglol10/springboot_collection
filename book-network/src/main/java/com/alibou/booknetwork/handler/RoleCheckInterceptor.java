package com.alibou.booknetwork.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collection;

// 특정 역할을 가진 사용자만 특정 API를 호출할 수 있도록 제한하고 싶은 경우
//사용자가 자신이 소유한 리소스만 수정하거나 삭제할 수 있도록 제한하고 싶은 경우

// 이 인터셉터는 모든 요청을 가로채서 사용자가 'ADMIN' 역할을 가지고 있는지 확인합니다. 만약 사용자가 'ADMIN' 역할을 가지고 있지 않다면, 요청은 거부되고 403 Forbidden 오류가 반환됩니다.

@Component
public class RoleCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return false;
        }

        return true;
    }
}
