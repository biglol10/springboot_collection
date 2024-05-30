package com.alibou.booknetwork.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter { // to make it understand that this class is a filter class

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // every time we have request, this method will be executed
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (request.getServletPath().contains("/api/v1/auth")) { // if the path is /auth, we don't need to check anything. Just pass and finish
            filterChain.doFilter(request, response);
            return;
        }
        final String authHeader = request.getHeader(AUTHORIZATION);
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        jwt = authHeader.substring(7);
        userEmail = jwtService.extractUsername(jwt);

        // SecurityContextHolder.getContext().getAuthentication() == null is a condition used in Spring Security to check if there is no current authentication information stored in the security context
        // In Spring Security, the SecurityContextHolder is a holder class that stores details about the security context of the current thread of execution. This includes details about the currently authenticated user, if any.
        // The getContext() method is used to retrieve the SecurityContext associated with the current thread. The SecurityContext can hold an Authentication object, which represents the token for an authentication request or for an authenticated principal (user).
        // The getAuthentication() method is used to retrieve the Authentication object from the SecurityContext. If no authentication information is available, this method will return null.
        // So, SecurityContextHolder.getContext().getAuthentication() == null is checking if there is no Authentication object stored in the SecurityContext, which would mean that the current user is not authenticated.
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // UsernamePasswordAuthenticationToken은 Spring Security에서 사용하는 인증 객체입니다. 이 객체는 사용자의 인증 정보를 나타내며, 인증 과정에서 사용자의 자격 증명을 저장하는 데 사용됩니다.
                // 아래 코드에서 UsernamePasswordAuthenticationToken 객체는 세 가지 매개변수를 받아 생성됩니다.
                // userDetails: UserDetails 객체는 Spring Security에서 사용자의 세부 정보를 나타냅니다. 이 객체는 사용자 이름, 비밀번호, 권한 등의 정보를 포함합니다.
                // null: 이 매개변수는 사용자의 자격 증명(일반적으로 비밀번호)을 나타냅니다. 여기서는 null이 전달되었는데, 이는 이미 인증이 완료되었음을 나타냅니다.
                // userDetails.getAuthorities(): 이는 사용자가 가진 권한의 목록을 나타냅니다. UserDetails 객체의 getAuthorities() 메소드를 통해 권한 목록을 가져옵니다.
                // 따라서, 이 코드는 인증된 사용자의 세부 정보와 권한을 가진 UsernamePasswordAuthenticationToken 객체를 생성합니다. 이 객체는 이후 보안 컨텍스트에 저장되어, 애플리케이션의 다른 부분에서 현재 인증된 사용자의 정보를 참조하는 데 사용될 수 있습니다.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // If I used CustomUserDetails like you provided above, when I run SecurityContextHolder.getContext().getAuthentication() or try to get principal, will I get object of class CustomUserDetails?
                // Yes, you will get an object of the CustomUserDetails class. The loadUserByUsername method in the UserDetailsService interface should return an object that implements the UserDetails interface. In this case, the CustomUserDetails class implements the UserDetails interface, so the object returned by the loadUserByUsername method will be an instance of the CustomUserDetails class.
                // CustomUserDetails customUserDetails = // load or create your user details
                //UsernamePasswordAuthenticationToken authentication =
                //        new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
                //SecurityContextHolder.getContext().setAuthentication(authentication);

                // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                //CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

                // build some details from request. extract some info to provide to token
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken); // manually authenticating the user
            }
        }
        filterChain.doFilter(request, response);
    }
}

//  SecurityContextHolder.getContext().getAuthentication() == null은 Spring Security에서 현재 보안 컨텍스트에 저장된 인증 정보가 없는지 확인하는 조건입니다.
//  Spring Security에서 SecurityContextHolder는 현재 실행 스레드의 보안 컨텍스트에 대한 세부 정보를 저장하는 홀더 클래스입니다.
//  이에는 현재 인증된 사용자에 대한 세부 정보가 포함될 수 있습니다.  getContext() 메소드는 현재 스레드와 연관된 SecurityContext를 검색하는 데 사용됩니다.
//  SecurityContext는 인증 요청의 토큰 또는 인증된 주체(사용자)를 나타내는 Authentication 객체를 보유할 수 있습니다.
//  getAuthentication() 메소드는 SecurityContext에서 Authentication 객체를 검색하는 데 사용됩니다. 인증 정보가 없는 경우, 이 메소드는 null을 반환합니다.
//  따라서, SecurityContextHolder.getContext().getAuthentication() == null은 SecurityContext에 Authentication 객체가 저장되어 있지 않은지 확인하는 것으로, 이는 현재 사용자가 인증되지 않았음을 의미합니다.
