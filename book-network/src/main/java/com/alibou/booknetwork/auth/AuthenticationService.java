package com.alibou.booknetwork.auth;

import com.alibou.booknetwork.email.EmailService;
import com.alibou.booknetwork.email.EmailTemplateName;
import com.alibou.booknetwork.role.RoleRepository;
import com.alibou.booknetwork.security.JwtService;
import com.alibou.booknetwork.user.Token;
import com.alibou.booknetwork.user.TokenRepository;
import com.alibou.booknetwork.user.User;
import com.alibou.booknetwork.user.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

/**
 * 인증 서비스
 * 
 * 이 서비스는 사용자 등록, 인증, 계정 활성화 등 인증 관련 비즈니스 로직을 처리합니다.
 * Spring Security와 통합되어 사용자 인증 및 토큰 관리를 담당합니다.
 * 
 * 주요 기능:
 * - 사용자 등록 및 이메일 검증
 * - JWT 기반 사용자 인증
 * - 계정 활성화 및 토큰 관리
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    // AuthenticationManager는 별도의 빈으로 생성되어야 합니다(BeansConfig에서 정의)
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    /**
     * 새 사용자를 등록합니다.
     * 
     * 이 메소드는 사용자 정보를 저장하고, 계정 활성화를 위한 검증 이메일을 발송합니다.
     * 기본적으로 사용자는 'USER' 역할을 부여받으며, 계정은 비활성화 상태로 생성됩니다.
     * 
     * @param registerationRequest 사용자 등록 정보
     * @throws MessagingException 이메일 발송 중 오류 발생 시
     */
    public void register(RegisterationRequest registerationRequest) throws MessagingException {
        var userRole = roleRepository.findByName("USER")
                // TODO: 예외 처리 개선 - 커스텀 예외 클래스 사용
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

        var user = User.builder()
                .firstname(registerationRequest.getFirstname())
                .lastname(registerationRequest.getLastname())
                .email(registerationRequest.getEmail())
                .password(passwordEncoder.encode(registerationRequest.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();

        userRepository.save(user);
        sendValidationEmail(user);
    }

    /**
     * 계정 활성화를 위한 검증 이메일을 발송합니다.
     * 
     * @param user 이메일을 발송할 사용자
     * @throws MessagingException 이메일 발송 중 오류 발생 시
     */
    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        
        // 이메일 서비스를 통해 활성화 링크가 포함된 이메일 발송
        emailService.sendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account activation"
        );
    }

    /**
     * 계정 활성화 토큰을 생성하고 저장합니다.
     * 
     * @param user 토큰을 생성할 사용자
     * @return 생성된 활성화 토큰
     */
    private String generateAndSaveActivationToken(User user) {
        // 6자리 활성화 코드 생성
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15)) // 토큰 유효 시간: 15분
                .user(user)
                .build();
        
        // 토큰 저장
        tokenRepository.save(token);
        return generatedToken;
    }

    /**
     * 보안적으로 안전한 활성화 코드를 생성합니다.
     * 
     * @param length 코드 길이
     * @return 생성된 활성화 코드
     */
    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder stringBuilder = new StringBuilder();
        // SecureRandom 사용 - 보안적으로 더 안전한 난수 생성기
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < length; i++) {
            // 무작위 인덱스 생성
            int randomIndex = secureRandom.nextInt(characters.length());
            // 해당 인덱스의 문자를 추가
            stringBuilder.append(characters.charAt(randomIndex));
        }
        return stringBuilder.toString();
    }

    /**
     * 사용자를 인증하고 JWT 토큰을 발급합니다.
     * 
     * @param request 인증 요청 정보(이메일, 비밀번호)
     * @return JWT 토큰이 포함된 인증 응답
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // AuthenticationManager를 통해 인증 수행
        // 이 과정에서 UserDetailsService가 호출되어 사용자 정보를 로드하고 비밀번호 검증이 이루어짐
        // this will take care of the whole authentication process if the username and password are correct, it will return the authentication otherwise it will throw an exception
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        ); // returns the authentication object (user details and credentials)
        
        // JWT 토큰에 포함할 추가 클레임 설정
        var claims = new HashMap<String, Object>();
        var user = (User) auth.getPrincipal();  // no need to get detais from userrepository because we already have the authentication object. We implemented Principal in User class
        // 사용자 전체 이름을 클레임에 추가
        claims.put("fullName", user.fullName());
        
        // JWT 토큰 생성
        var jwtToken = jwtService.generateToken(claims, user);
        return AuthenticationResponse.builder()
                .token(jwtToken).build();
    }

    /**
     * 계정 활성화 토큰을 검증하고 사용자 계정을 활성화합니다.
     * 토큰이 만료된 경우 새 토큰을 생성하여 이메일로 발송합니다.
     * 
     * @param token 활성화 토큰
     * @throws MessagingException 이메일 발송 중 오류 발생 시
     */
    // @Transactional 주석 해제 권장 - 토큰 검증과 사용자 활성화를 단일 트랜잭션으로 처리
    public void activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found")); // TODO: 커스텀 예외 정의 필요

        // 토큰 만료 확인
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            // 만료된 경우 새 토큰 발송
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation Token expired. A new token has been sent to the same email address");
        }
        
        // 사용자 조회
        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 계정 활성화
        user.setEnabled(true);
        userRepository.save(user);
        
        // 토큰 검증 시간 기록
        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 보안 강화:
 *    - OWASP 가이드라인에 따른 비밀번호 정책 구현
 *    - 무차별 대입 공격 방지를 위한 로그인 시도 제한
 *    - 2FA(Two-Factor Authentication) 구현
 *    
 *    예시:
 *    private boolean isValidPassword(String password) {
 *        // 최소 8자, 대소문자, 숫자, 특수문자 포함 검증
 *        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
 *        return password.matches(regex);
 *    }
 * 
 * 2. 소셜 로그인 통합:
 *    - OAuth2 인증 흐름 구현
 *    - Google, Facebook, GitHub 등 다양한 소셜 로그인 지원
 *    - 기존 계정과 소셜 계정 연동
 *    
 *    예시:
 *    @Autowired
 *    private OAuth2UserService oAuth2UserService;
 *    
 *    public AuthenticationResponse authenticateWithOAuth2(String provider, String code) {
 *        OAuth2User oAuth2User = oAuth2UserService.processCode(provider, code);
 *        User user = findOrCreateUser(oAuth2User);
 *        return generateTokenResponse(user);
 *    }
 * 
 * 3. 리프레시 토큰 구현:
 *    - 액세스 토큰과 리프레시 토큰 분리
 *    - 리프레시 토큰 순환(rotation) 전략 적용
 *    - 보안 이벤트 기반 토큰 무효화
 *    
 *    예시:
 *    public TokenResponse refreshToken(String refreshToken) {
 *        // 리프레시 토큰 검증
 *        // 새 액세스 토큰 발급
 *        // 리프레시 토큰 순환
 *        return new TokenResponse(newAccessToken, newRefreshToken);
 *    }
 * 
 * 4. 계정 관리 기능 확장:
 *    - 비밀번호 재설정 워크플로우
 *    - 이메일 변경 시 재검증
 *    - 계정 삭제 및 개인정보 처리
 *    
 *    예시:
 *    public void initiatePasswordReset(String email) {
 *        User user = userRepository.findByEmail(email)
 *            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
 *        String resetToken = generateResetToken(user);
 *        sendPasswordResetEmail(user, resetToken);
 *    }
 * 
 * 5. 로깅 및 감사:
 *    - 인증 이벤트 감사 로그 구현
 *    - 보안 위반 시도 감지 및 알림
 *    - 인증 패턴 분석 및 이상 징후 감지
 *    
 *    예시:
 *    @Autowired
 *    private AuthAuditService auditService;
 *    
 *    public AuthenticationResponse authenticate(AuthenticationRequest request) {
 *        try {
 *            // 기존 인증 로직
 *            auditService.logSuccessfulAuthentication(user.getId(), request.getClientInfo());
 *            return response;
 *        } catch (Exception e) {
 *            auditService.logFailedAuthentication(request.getEmail(), request.getClientInfo());
 *            throw e;
 *        }
 *    }
 * 
 * 6. 세션 관리 개선:
 *    - 동시 세션 제어
 *    - 세션 만료 및 갱신 전략
 *    - 클라이언트 정보 기반 세션 검증
 * 
 * 7. 분산 환경 지원:
 *    - 토큰 저장소 Redis 통합
 *    - 캐싱 전략 구현
 *    - 수평적 확장 고려(Stateless 설계)
 * 
 * 8. 권한 관리 고도화:
 *    - 동적 역할 부여 시스템
 *    - 권한 상속 및 계층 구조
 *    - 세분화된 권한 제어(RBAC/ABAC)
 */
