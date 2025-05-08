package com.alibou.booknetwork.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT(JSON Web Token) 서비스
 * 
 * 이 서비스는 JWT 토큰의 생성, 검증 및 파싱을 담당합니다.
 * Spring Security와 통합되어 인증된 사용자를 위한 토큰 기반 인증을 제공합니다.
 */
@Service
public class JwtService {
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration; // JWT 토큰의 만료 시간(밀리초)
    
    @Value("${application.security.jwt.secret-key}")
    private String secretKey; // JWT 토큰 서명에 사용되는 비밀 키

    /**
     * 사용자 정보를 기반으로 JWT 토큰을 생성합니다.
     * 추가 클레임 없이 기본 토큰을 생성합니다.
     * 
     * @param userDetails 사용자 상세 정보
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * 추가 클레임과 사용자 정보를 기반으로 JWT 토큰을 생성합니다.
     * 
     * @param claims 토큰에 포함할 추가 클레임
     * @param userDetails 사용자 상세 정보
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateToken(Map<String, Object> claims, UserDetails userDetails) {
        return buildToken(claims, userDetails, jwtExpiration);
    }

    /**
     * JWT 토큰을 실제로 구성하고 빌드하는 내부 메소드입니다.
     * 
     * @param extraClaims 토큰에 포함할 추가 클레임
     * @param userDetails 사용자 상세 정보
     * @param jwtExpiration 토큰 만료 시간(밀리초)
     * @return 구성된 JWT 토큰 문자열
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long jwtExpiration) {
        var authorities = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority) // 사용자의 권한 정보를 추출합니다.
                .toList();
        return Jwts.builder()
                .setClaims(extraClaims) // 추가 클레임 설정
                .setSubject(userDetails.getUsername()) // 사용자 식별자(username) 설정
                .setIssuedAt(new Date(System.currentTimeMillis())) // 토큰 발행 시간 설정
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // 토큰 만료 시간 설정
                .claim("authorities", authorities) // 사용자 권한 정보 설정
                .signWith(getSignInKey()) // 비밀 키로 토큰 서명
                .compact(); // 토큰을 문자열로 변환
    }

    /**
     * 주어진 토큰이 유효한지 검증합니다.
     * 토큰에서 추출한 사용자명과 제공된 사용자 정보가 일치하는지,
     * 그리고 토큰이 만료되지 않았는지 확인합니다.
     * 
     * @param token 검증할 JWT 토큰
     * @param userDetails 사용자 상세 정보
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * 토큰이 만료되었는지 확인합니다.
     * 
     * @param token 검사할 JWT 토큰
     * @return 토큰이 만료되었으면 true, 그렇지 않으면 false
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 토큰에서 만료 시간을 추출합니다.
     * 
     * @param token JWT 토큰
     * @return 토큰의 만료 시간
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 토큰에서 사용자명(주체)을 추출합니다.
     * 
     * Claims::getSubject는 Java의 메소드 참조로, io.jsonwebtoken 패키지의 Claims 클래스의 getSubject 메소드를 가리킵니다.
     * 이 메소드는 JWT의 주체(subject)를 반환하며, 이 컨텍스트에서는 사용자명입니다.
     * 
     * @param token JWT 토큰
     * @return 추출된 사용자명
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject); // .setSubject(userDetails.getUsername()) 때문에 이렇게 적음
    }

    /**
     * 토큰에서 특정 클레임을 추출합니다.
     * 
     * 이 메소드는 토큰과 Claims를 입력으로 받아 T 타입의 값을 반환하는 Function을 매개변수로 받습니다.
     * 메소드는 토큰에서 모든 클레임을 추출한 다음, 제공된 함수를 이 클레임에 적용합니다.
     * 
     * extractUsername의 문맥에서, claimsResolver 함수는 Claims::getSubject이므로, 
     * extractClaim 메소드는 JWT의 주체(사용자명)를 반환합니다.
     * 
     * @param token JWT 토큰
     * @param claimsResolver 클레임에서 원하는 정보를 추출하는 함수
     * @param <T> 반환 값의 타입
     * @return 추출된 클레임 값
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 토큰에서 모든 클레임을 추출합니다.
     * 
     * @param token JWT 토큰
     * @return 토큰의 모든 클레임
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey()) // 토큰 검증을 위한 서명 키 설정
                .build()
                .parseClaimsJws(token) // 토큰 파싱 및 서명 검증
                .getBody(); // 클레임(페이로드) 반환
    }

    /**
     * 서명 키를 생성합니다.
     * 비밀 키를 디코딩하고 HMAC-SHA 알고리즘에 적합한 키로 변환합니다.
     * 
     * @return JWT 서명에 사용될 키
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey); // Base64로 인코딩된 비밀 키를 디코딩
        return Keys.hmacShaKeyFor(keyBytes); // HMAC-SHA 알고리즘에 사용할 키 생성
    }
}

/**
 * 고급 사용 사례 및 시니어 개발자를 위한 추가 정보
 * 
 * 1. 토큰 새로 고침(Token Refresh) 구현:
 *    - 액세스 토큰과 리프레시 토큰을 분리하여 보안 강화
 *    - 리프레시 토큰은 더 긴 만료 시간을 가지며, 새 액세스 토큰 발급에 사용
 *    - 리프레시 토큰 순환(rotation) 구현으로 보안 강화
 * 
 * 2. 토큰 블랙리스팅:
 *    - 로그아웃 또는 보안 위반 시 토큰을 블랙리스트에 추가
 *    - Redis와 같은 인메모리 저장소를 사용하여 블랙리스트 관리
 *    - 토큰 ID(jti)를 사용하여 특정 토큰 무효화
 * 
 * 3. 클레임 커스터마이징:
 *    - 비즈니스 로직에 필요한 추가 정보를 클레임에 포함
 *    - 사용자 역할, 권한, 테넌트 ID 등 컨텍스트 정보 추가
 *    - 클레임 암호화 고려(중요 정보 포함 시)
 * 
 * 4. 고급 보안 설정:
 *    - 비대칭 키(RSA)를 사용한 토큰 서명
 *    - JWE(JSON Web Encryption)를 사용한 페이로드 암호화
 *    - 토큰 교체(rotation) 전략 구현
 * 
 * 5. 토큰 관리 최적화:
 *    - 토큰 크기 최소화(클레임 제한)
 *    - 토큰 검증 캐싱 구현
 *    - 마이크로서비스 환경에서의 토큰 검증 전략
 * 
 * 6. 보안 모범 사례:
 *    - 토큰 수명 제한(15-30분 권장)
 *    - 비밀 키 정기적 교체
 *    - 환경별 다른 비밀 키 사용
 *    - Vault와 같은 도구로 비밀 키 안전하게 관리
 * 
 * 7. 통합 시나리오:
 *    - OAuth2/OIDC와의 통합
 *    - 소셜 로그인 시스템과의 연동
 *    - API 게이트웨이와의 통합
 * 
 * 8. 확장성 고려사항:
 *    - 다중 인증 서버 환경에서의 토큰 검증
 *    - 클라우드 네이티브 환경에서의 키 관리
 *    - 컨테이너화된 환경에서의 비밀 관리
 */
