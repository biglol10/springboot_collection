package com.alibou.booknetwork.security;

import com.alibou.booknetwork.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository repository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException {
        return repository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + userEmail));
    }
}

// loadUserByUsername 메서드가 UserDetails가 아닌 다른 클래스를 반환하도록 변경하면, getPrincipal() 메서드를 호출했을 때 해당 클래스의 인스턴스가 반환됩니다.
// 하지만, 이렇게 변경하려면 Authentication 객체를 생성할 때 해당 클래스의 인스턴스를 주체(principal)로 설정해야 합니다. 그리고 이 클래스는 Spring Security가 인증과 권한 부여를 수행하는 데 필요한 정보를 제공해야 합니다.
// 예를 들어, CustomUserDetails라는 클래스를 사용하려면 다음과 같이 할 수 있습니다:
// CustomUserDetails customUserDetails = // load or create your user details
//UsernamePasswordAuthenticationToken authentication =
//        new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
//SecurityContextHolder.getContext().setAuthentication(authentication);
// Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

//`AuthenticationProvider`, 특히 `DaoAuthenticationProvider`가 데이터베이스에서 사용자 정보를 로드하고 비밀번호를 검증할 때 사용하는 칼럼은 주로 다음과 같은 방식으로 결정됩니다:
//
//        1. **사용자 구현체의 `UserDetailsService`**:
//        - 스프링 시큐리티는 `UserDetailsService` 인터페이스를 통해 데이터베이스에서 사용자 정보를 로드하는 매커니즘을 제공합니다.
//        - 개발자는 이 인터페이스의 `loadUserByUsername(String username)` 메서드를 구현하여 특정 사용자를 찾는 로직을 정의합니다.
//        - 이 메서드 내에서, 사용자의 "username"을 매개변수로 받아 데이터베이스에서 해당 사용자를 조회합니다. 여기서 "username"은 일반적으로 사용자를 식별하는 고유한 값으로, 데이터베이스의 특정 칼럼(예: `username`, `email` 등)과 매핑됩니다.
//
//        2. **`UserDetails` 인터페이스**:
//        - `loadUserByUsername` 메서드는 `UserDetails` 인터페이스를 구현한 객체를 반환합니다.
//        - `UserDetails`는 사용자의 정보(예: 사용자명, 비밀번호, 권한 등)를 담고 있습니다.
//        - 여기서 비밀번호는 `UserDetails` 객체의 `getPassword()` 메서드를 통해 얻을 수 있으며, 이는 데이터베이스의 비밀번호 칼럼과 매핑됩니다.
//
//        3. **데이터베이스와의 매핑**:
//        - 실제 데이터베이스 테이블과 객체 간의 매핑은 `JPA`, `Hibernate`, `MyBatis` 등의 ORM(Object-Relational Mapping) 라이브러리를 사용하여 설정할 수 있습니다.
//        - 예를 들어, `JPA`를 사용하는 경우, 엔티티 클래스에 `@Entity` 어노테이션을 사용하고, 각 필드에 `@Column` 어노테이션을 사용하여 데이터베이스 칼럼과 매핑합니다.
//
//        결론적으로, `AuthenticationProvider`가 어떤 데이터베이스 칼럼을 보는지는 개발자가 구현한 `UserDetailsService`의 `loadUserByUsername` 메서드와, 해당 사용자 정보 클래스(예: `UserDetails` 구현체)와 데이터베이스 테이블 간의 매핑 설정에 따라 달라집니다.
//
//        이런 자료를 참고했어요.
//        [1] Naver Blog - Spring Security(5) - AuthenticationProvider : 네이버 블로그 (https://blog.naver.com/PostView.naver?blogId=jieuni4u&logNo=221811830916)
//        [2] velog - 사용자 추가정보 저장하기 - Spring UserDetailsService의 구현 (https://velog.io/@tabi4645/Spring-UserDetailService)
//        [3] 네이버 블로그 - 12회차_Spring Security 로그인 방법(데이터베이스 연동) (https://m.blog.naver.com/sam_sist/220964537132)
//        [4] DotNetNote - 스프링 시큐리티 (https://www.dotnetnote.com/docs/spring-boot/spring-security/)
