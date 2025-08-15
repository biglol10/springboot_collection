package com.alibou.booknetwork.security;

import com.alibou.booknetwork.security.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT(JSON Web Token) 서비스 - 엔터프라이즈급 강화 버전
 * 
 * 이 서비스는 JWT 토큰의 생성, 검증 및 파싱을 담당합니다.
 * SSGD 스타일의 엔터프라이즈 보안 기능을 포함하여 토큰 기반 인증을 제공합니다.
 * 
 * 주요 기능:
 * - 강화된 토큰 검증 (만료, 서명, 형식, 클레임 검증)
 * - 상세한 예외 처리 및 로깅
 * - 토큰 ID 추적 (jti claim)
 * - IP 주소 및 User-Agent 검증
 * - 권한 정보 포함 토큰 생성
 * - 토큰 블랙리스트 지원 (향후 확장 가능)
 */
@Slf4j
@Service
public class JwtService {
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration; // JWT 토큰의 만료 시간(밀리초)
    
    @Value("${application.security.jwt.secret-key}")
    private String secretKey; // JWT 토큰 서명에 사용되는 비밀 키
    
    @Value("${application.security.jwt.issuer:book-network}")
    private String issuer; // JWT 발행자
    
    @Value("${application.security.jwt.audience:book-network-users}")
    private String audience; // JWT 대상자
    
    // 표준 클레임 명
    public static final String CLAIM_AUTHORITIES = "authorities";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_IP_ADDRESS = "ipAddress";
    public static final String CLAIM_USER_AGENT = "userAgent";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_DEVICE_ID = "deviceId";
    
    // 토큰 타입
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

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
     * @param extraClaims 토큰에 포함할 추가 클레임
     * @param userDetails 사용자 상세 정보
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return generateEnhancedToken(extraClaims, userDetails, null, null, null);
    }

    /**
     * 엔터프라이즈급 토큰 생성 - 보안 정보 포함
     * 
     * @param extraClaims 추가 클레임
     * @param userDetails 사용자 정보
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @param deviceId 디바이스 ID (선택적)
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateEnhancedToken(Map<String, Object> extraClaims, 
                                      UserDetails userDetails, 
                                      String ipAddress, 
                                      String userAgent, 
                                      String deviceId) {
        return buildEnhancedToken(extraClaims, userDetails, jwtExpiration, 
                                TOKEN_TYPE_ACCESS, ipAddress, userAgent, deviceId);
    }

    /**
     * 리프레시 토큰 생성
     * 
     * @param userDetails 사용자 정보
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @return 생성된 리프레시 토큰 문자열
     */
    public String generateRefreshToken(UserDetails userDetails, String ipAddress, String userAgent) {
        Map<String, Object> claims = new HashMap<>();
        return buildEnhancedToken(claims, userDetails, jwtExpiration * 7, // 7배 긴 수명
                                TOKEN_TYPE_REFRESH, ipAddress, userAgent, null);
    }

    /**
     * 엔터프라이즈급 JWT 토큰을 실제로 구성하고 빌드하는 내부 메소드
     * 
     * @param extraClaims 토큰에 포함할 추가 클레임
     * @param userDetails 사용자 상세 정보
     * @param tokenExpiration 토큰 만료 시간(밀리초)
     * @param tokenType 토큰 타입 (ACCESS, REFRESH)
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @param deviceId 디바이스 ID
     * @return 구성된 JWT 토큰 문자열
     */
    private String buildEnhancedToken(Map<String, Object> extraClaims, 
                                    UserDetails userDetails, 
                                    long tokenExpiration,
                                    String tokenType,
                                    String ipAddress, 
                                    String userAgent, 
                                    String deviceId) {
        
        Date now = new Date(System.currentTimeMillis());
        Date expiryDate = new Date(now.getTime() + tokenExpiration);
        String tokenId = UUID.randomUUID().toString(); // 고유 토큰 ID
        
        // 사용자 권한 정보 추출
        List<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        
        // User ID 추출 (User 엔티티에서 ID 가져오기, 없으면 null)
        Integer userId = null;
        if (userDetails instanceof com.alibou.booknetwork.user.User) {
            userId = ((com.alibou.booknetwork.user.User) userDetails).getId();
        }
        
        var builder = Jwts.builder()
                // 표준 클레임 설정
                .setId(tokenId) // JWT ID (jti)
                .setIssuer(issuer) // 발행자
                .setAudience(audience) // 대상자
                .setSubject(userDetails.getUsername()) // 주체 (사용자명/이메일)
                .setIssuedAt(now) // 발행 시간
                .setExpiration(expiryDate) // 만료 시간
                .setNotBefore(now) // 유효 시작 시간
                
                // 커스텀 클레임 설정
                .claim(CLAIM_AUTHORITIES, authorities) // 권한 정보
                .claim(CLAIM_TOKEN_TYPE, tokenType); // 토큰 타입
        
        // 선택적 클레임 추가
        if (userId != null) {
            builder.claim(CLAIM_USER_ID, userId);
        }
        if (ipAddress != null && !ipAddress.isEmpty()) {
            builder.claim(CLAIM_IP_ADDRESS, ipAddress);
        }
        if (userAgent != null && !userAgent.isEmpty()) {
            builder.claim(CLAIM_USER_AGENT, userAgent);
        }
        if (deviceId != null && !deviceId.isEmpty()) {
            builder.claim(CLAIM_DEVICE_ID, deviceId);
        }
        
        // 추가 클레임 설정
        if (extraClaims != null && !extraClaims.isEmpty()) {
            builder.setClaims(extraClaims);
        }
        
        String token = builder
                .signWith(getSignInKey()) // 비밀 키로 토큰 서명
                .compact(); // 토큰을 문자열로 변환
        
        log.debug("Generated {} token for user: {}, tokenId: {}, expires: {}", 
                 tokenType, userDetails.getUsername(), tokenId, expiryDate);
        
        return token;
    }

    /**
     * 기존 호환성을 위한 레거시 buildToken 메서드
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long jwtExpiration) {
        return buildEnhancedToken(extraClaims, userDetails, jwtExpiration, 
                                TOKEN_TYPE_ACCESS, null, null, null);
    }

    /**
     * 엔터프라이즈급 토큰 검증 - 다층 보안 검사
     * 
     * @param token 검증할 JWT 토큰
     * @param userDetails 사용자 상세 정보
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            // 1. 기본 토큰 구조 및 서명 검증
            Claims claims = extractAllClaims(token);
            
            // 2. 사용자명 일치 확인
            final String tokenUsername = claims.getSubject();
            if (!tokenUsername.equals(userDetails.getUsername())) {
                log.warn("Token username mismatch. Expected: {}, Found: {}", 
                        userDetails.getUsername(), tokenUsername);
                return false;
            }
            
            // 3. 토큰 만료 확인
            if (isTokenExpired(token)) {
                log.debug("Token expired for user: {}", tokenUsername);
                return false;
            }
            
            // 4. 토큰 타입 확인 (ACCESS 토큰인지)
            String tokenType = (String) claims.get(CLAIM_TOKEN_TYPE);
            if (tokenType != null && !TOKEN_TYPE_ACCESS.equals(tokenType)) {
                log.warn("Invalid token type for authentication. Expected: {}, Found: {}", 
                        TOKEN_TYPE_ACCESS, tokenType);
                return false;
            }
            
            // 5. 발행자 및 대상자 검증
            if (!issuer.equals(claims.getIssuer())) {
                log.warn("Invalid token issuer. Expected: {}, Found: {}", issuer, claims.getIssuer());
                throw TokenException.issuerInvalid(issuer, claims.getIssuer());
            }
            
            // 6. 토큰 유효 시작 시간 확인 (nbf)
            Date notBefore = claims.getNotBefore();
            if (notBefore != null && notBefore.after(new Date())) {
                log.warn("Token not yet valid. Not before: {}", notBefore);
                throw TokenException.notYetValid("Token is not yet valid. Valid from: " + notBefore);
            }
            
            log.debug("Token validation successful for user: {}", tokenUsername);
            return true;
            
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during token validation: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * IP 주소 검증을 포함한 토큰 유효성 검사
     * 
     * @param token 검증할 JWT 토큰
     * @param userDetails 사용자 상세 정보
     * @param clientIpAddress 클라이언트 IP 주소
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean isTokenValidWithIpCheck(String token, UserDetails userDetails, String clientIpAddress) {
        if (!isTokenValid(token, userDetails)) {
            return false;
        }
        
        try {
            Claims claims = extractAllClaims(token);
            String tokenIpAddress = (String) claims.get(CLAIM_IP_ADDRESS);
            
            // IP 주소가 토큰에 저장되어 있고, 현재 요청의 IP와 다른 경우
            if (tokenIpAddress != null && !tokenIpAddress.equals(clientIpAddress)) {
                log.warn("IP address mismatch. Token IP: {}, Request IP: {}, User: {}", 
                        tokenIpAddress, clientIpAddress, userDetails.getUsername());
                throw TokenException.ipMismatch(tokenIpAddress, clientIpAddress);
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error during IP validation: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 토큰 타입별 검증
     * 
     * @param token 검증할 JWT 토큰
     * @param expectedType 예상되는 토큰 타입
     * @return 토큰이 올바른 타입이면 true
     */
    public boolean isTokenTypeValid(String token, String expectedType) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = (String) claims.get(CLAIM_TOKEN_TYPE);
            return expectedType.equals(tokenType);
        } catch (Exception e) {
            log.warn("Error validating token type: {}", e.getMessage());
            return false;
        }
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
     * 토큰에서 모든 클레임을 추출합니다. (엔터프라이즈급 예외 처리 포함)
     * 
     * @param token JWT 토큰
     * @return 토큰의 모든 클레임
     * @throws JwtException 토큰 파싱 중 오류 발생 시
     */
    private Claims extractAllClaims(String token) throws JwtException {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey()) // 토큰 검증을 위한 서명 키 설정
                    .requireIssuer(issuer) // 발행자 검증
                    .requireAudience(audience) // 대상자 검증
                    .build()
                    .parseClaimsJws(token) // 토큰 파싱 및 서명 검증
                    .getBody(); // 클레임(페이로드) 반환
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw e;
        }
    }

    // ================================
    // 추가 유틸리티 메서드들
    // ================================

    /**
     * 토큰에서 토큰 ID(jti)를 추출합니다.
     * 
     * @param token JWT 토큰
     * @return 토큰 ID
     */
    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * 토큰에서 사용자 ID를 추출합니다.
     * 
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Integer extractUserId(String token) {
        return extractClaim(token, claims -> (Integer) claims.get(CLAIM_USER_ID));
    }

    /**
     * 토큰에서 권한 목록을 추출합니다.
     * 
     * @param token JWT 토큰
     * @return 권한 목록
     */
    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get(CLAIM_AUTHORITIES));
    }

    /**
     * 토큰에서 IP 주소를 추출합니다.
     * 
     * @param token JWT 토큰
     * @return IP 주소
     */
    public String extractIpAddress(String token) {
        return extractClaim(token, claims -> (String) claims.get(CLAIM_IP_ADDRESS));
    }

    /**
     * 토큰에서 User-Agent를 추출합니다.
     * 
     * @param token JWT 토큰
     * @return User-Agent
     */
    public String extractUserAgent(String token) {
        return extractClaim(token, claims -> (String) claims.get(CLAIM_USER_AGENT));
    }

    /**
     * 토큰에서 토큰 타입을 추출합니다.
     * 
     * @param token JWT 토큰
     * @return 토큰 타입
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> (String) claims.get(CLAIM_TOKEN_TYPE));
    }

    /**
     * 토큰이 곧 만료되는지 확인합니다. (임계값 기반)
     * 
     * @param token JWT 토큰
     * @param thresholdMinutes 만료 임계값 (분)
     * @return 임계값 내에 만료되면 true
     */
    public boolean isTokenNearExpiry(String token, int thresholdMinutes) {
        try {
            Date expiration = extractExpiration(token);
            Date threshold = new Date(System.currentTimeMillis() + (thresholdMinutes * 60 * 1000L));
            return expiration.before(threshold);
        } catch (Exception e) {
            log.warn("Error checking token expiry: {}", e.getMessage());
            return true; // 오류 시 만료된 것으로 간주
        }
    }

    /**
     * 토큰 정보를 맵으로 반환합니다. (디버깅용)
     * 
     * @param token JWT 토큰
     * @return 토큰 정보 맵
     */
    public Map<String, Object> getTokenInfo(String token) {
        Map<String, Object> tokenInfo = new HashMap<>();
        
        try {
            Claims claims = extractAllClaims(token);
            
            tokenInfo.put("tokenId", claims.getId());
            tokenInfo.put("subject", claims.getSubject());
            tokenInfo.put("issuer", claims.getIssuer());
            tokenInfo.put("audience", claims.getAudience());
            tokenInfo.put("issuedAt", claims.getIssuedAt());
            tokenInfo.put("expiration", claims.getExpiration());
            tokenInfo.put("notBefore", claims.getNotBefore());
            
            // 커스텀 클레임
            tokenInfo.put("authorities", claims.get(CLAIM_AUTHORITIES));
            tokenInfo.put("userId", claims.get(CLAIM_USER_ID));
            tokenInfo.put("tokenType", claims.get(CLAIM_TOKEN_TYPE));
            tokenInfo.put("ipAddress", claims.get(CLAIM_IP_ADDRESS));
            tokenInfo.put("userAgent", claims.get(CLAIM_USER_AGENT));
            tokenInfo.put("deviceId", claims.get(CLAIM_DEVICE_ID));
            
            tokenInfo.put("expired", isTokenExpired(token));
            tokenInfo.put("nearExpiry", isTokenNearExpiry(token, 15)); // 15분 임계값
            
        } catch (Exception e) {
            tokenInfo.put("error", e.getMessage());
            tokenInfo.put("valid", false);
        }
        
        return tokenInfo;
    }

    /**
     * 안전한 토큰 파싱 (예외 무시)
     * 
     * @param token JWT 토큰
     * @return 파싱 성공 시 Claims, 실패 시 null
     */
    public Claims parseTokenSafely(String token) {
        try {
            return extractAllClaims(token);
        } catch (Exception e) {
            log.debug("Failed to parse token safely: {}", e.getMessage());
            return null;
        }
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
