package com.biglol.springsecuritypractice.jwt;

import com.biglol.springsecuritypractice.user.User;
import com.biglol.springsecuritypractice.user.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

// JWT token이 들어왔을 때 그것을 인가시켜주는 filter

/**
 * JWT를 이용한 인증
 */
public class JwtAuthorizationFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;

    public JwtAuthorizationFilter(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        String token = null;

        // 1. Cookie에서 JWT Token을 구함
        // 2. JWT Token을 파싱하여 username을 구함
        // 3. username으로 User를 구하고 Authenticcation을 생성
        // 4. 생성된 Authentication을 SecurityContext에 넣음
        // 5. Exception이 발생하면 응답의 쿠키를 null로 변경

        try {
            // cookie에서 JWT token을 가져옴
            token = Arrays.stream(request.getCookies())
                    .filter(cookie -> cookie.getName().equals(JwtProperties.COOKIE_NAME)).findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        } catch (Exception ignored) {}


        if (token != null) {
            try {
                Authentication authentication = getUsernamePasswordAuthenticationToken(token);
                SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContextHolder에 넣어줌
            } catch (Exception e) {
                Cookie cookie = new Cookie(JwtProperties.COOKIE_NAME, null);
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * JWT 토큰으로 User를 찾아서 UsernamePasswordAuthenticationToken를 만들어서 반환한다.
     * User가 없다면 null
     */
    private Authentication getUsernamePasswordAuthenticationToken(String token) {
        String username = JwtUtils.getUsername(token);
        if (username != null) {
            User user = userRepository.findByUsername(username); // 유저를 유저명으로 찾음
            return new UsernamePasswordAuthenticationToken(
                    user, // principal
                    null,
                    user.getAuthorities()
            );
        }
        return null;
    }
}
