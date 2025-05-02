package com.alibou.booknetwork.security;

import com.alibou.booknetwork.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security를 위한 UserDetailsService 구현체
 * 
 * 이 서비스는 인증 과정에서 사용자 정보를 데이터베이스에서 로드하는 역할을 담당합니다.
 * UserDetailsService는 Spring Security의 핵심 인터페이스로, 사용자명(일반적으로 이메일이나 ID)을 
 * 기반으로 UserDetails 객체를 로드하는 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository repository;

    /**
     * 사용자명(이메일)을 기반으로 사용자 정보를 로드합니다.
     * 
     * 이 메서드는 AuthenticationProvider에 의해 호출되어 인증 과정에서 사용됩니다.
     * 사용자를 찾을 수 없는 경우 UsernameNotFoundException을 발생시킵니다.
     * 
     * @param userEmail 사용자 이메일 (사용자 식별자)
     * @return UserDetails 타입의 사용자 정보 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우 발생
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException {
        return repository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + userEmail));
    }
}

/**
 * UserDetailsService와 Spring Security 인증 과정에 대한 추가 정보
 * 
 * 1. UserDetailsService의 역할:
 *    - Spring Security의 인증 시스템에서 사용자 정보를 로드하는 핵심 인터페이스입니다.
 *    - loadUserByUsername(String username) 메서드를 구현하여 특정 사용자를 찾는 로직을 정의합니다.
 *    - 보통 이 메서드는 username(또는 email) 매개변수를 사용하여 데이터베이스에서 사용자를 조회합니다.
 * 
 * 2. 사용자 엔티티와 UserDetails 인터페이스:
 *    - 일반적으로 사용자 엔티티 클래스는 UserDetails 인터페이스를 구현합니다.
 *    - UserDetails는 사용자명, 비밀번호, 권한 등 인증에 필요한 정보를 Spring Security에 제공합니다.
 *    - getPassword() 메서드는 사용자의 암호화된 비밀번호를 반환하며, 이는 AuthenticationProvider가
 *      입력된 비밀번호와 비교하는 데 사용됩니다.
 * 
 * 3. 커스텀 UserDetails 구현:
 *    - 기본 UserDetails 구현 대신 커스텀 구현체를 사용할 수 있습니다.
 *    - 예를 들어, 사용자 ID, 이메일, 프로필 정보 등 추가 필드를 포함할 수 있습니다.
 * 
 * 4. SecurityContext와의 통합:
 *    - 인증 성공 후, loadUserByUsername()이 반환한 UserDetails 객체(또는 커스텀 구현체)는
 *      Authentication 객체의 principal로 설정되어 SecurityContext에 저장됩니다.
 *    - 이후 SecurityContextHolder.getContext().getAuthentication().getPrincipal()을 
 *      통해 현재 인증된 사용자의 정보에 접근할 수 있습니다.
 * 
 * 5. 커스텀 UserDetails 사용 예시:
 *    // 커스텀 UserDetails 객체 생성 또는 로드
 *    CustomUserDetails customUserDetails = userDetailsService.loadUserByUsername(username);
 *    
 *    // Authentication 객체 생성 및 SecurityContext에 설정
 *    UsernamePasswordAuthenticationToken authentication = 
 *        new UsernamePasswordAuthenticationToken(
 *            customUserDetails,
 *            null,  // credentials (인증 후에는 보안상의 이유로 null로 설정)
 *            customUserDetails.getAuthorities()
 *        );
 *    SecurityContextHolder.getContext().setAuthentication(authentication);
 *    
 *    // 현재 인증된 사용자 정보 접근
 *    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
 *    CustomUserDetails currentUser = (CustomUserDetails) currentAuth.getPrincipal();
 * 
 * 6. 데이터베이스 매핑과 AuthenticationProvider:
 *    - AuthenticationProvider는 사용자 인증 로직을 처리합니다.
 *    - 기본 구현체인 DaoAuthenticationProvider는 UserDetailsService를 사용하여 사용자를 조회하고,
 *      PasswordEncoder를 사용하여 비밀번호를 검증합니다.
 *    - 어떤 데이터베이스 컬럼이 사용되는지는 UserDetailsService 구현과 엔티티-데이터베이스 매핑에 따라 결정됩니다.
 *    - JPA, Hibernate 등의 ORM을 사용하여 엔티티와 데이터베이스 테이블 간의 매핑을 설정할 수 있습니다.
 */
