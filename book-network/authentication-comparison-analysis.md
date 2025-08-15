# Spring Security 인증 방식 비교 분석

## 프로젝트 개요
- **SSGD_API_REAL (엔터프라이즈 프로젝트)**: 대규모 전자상거래 플랫폼의 Back Office 시스템
- **Book-Network (학습 프로젝트)**: FastCampus Spring Boot 강의 교재

---

## 1. 인증 아키텍처 비교

### SSGD_API_REAL (엔터프라이즈급)
```
Request → AuthenticationFilter → JWT 검증 → Redis 세션 확인 → 토큰 대조 → SecurityContext 설정
```

**특징:**
- **이중 검증 시스템**: JWT + Redis 세션 정보 대조
- **상태 관리**: Redis를 통한 세션 캐싱
- **시스템 구분**: BO/PO/FO 다중 시스템 지원
- **Mock 로그인**: 개발환경 전용 인증 우회

### Book-Network (교육용)
```
Request → JwtFilter → JWT 검증 → UserDetailsService → SecurityContext 설정
```

**특징:**
- **단순 JWT 검증**: 토큰 자체 유효성만 확인
- **무상태**: 완전한 Stateless 방식
- **Mock 로그인**: 개발환경 헤더 기반 인증

---

## 2. 보안 컴포넌트 상세 비교

### 2.1 필터 구조

| 구분 | SSGD_API_REAL | Book-Network |
|------|---------------|--------------|
| **필터 클래스** | `AuthenticationFilter` | `JwtFilter` |
| **상속** | `OncePerRequestFilter` | `OncePerRequestFilter` |
| **검증 단계** | JWT → Redis → 토큰 대조 | JWT → UserDetails |
| **예외 처리** | 상세한 HTTP 상태코드 | 기본적인 401/500 처리 |

### 2.2 인증 제공자

#### SSGD_API_REAL - AuthenticationProvider
```java
// 엔터프라이즈 기능
public Authentication getAuthentication(
    String userId, String sessionId, String authorityGroupCode,
    String[] authorities, String brandCode, String storeCode,
    String shopId, String channelId, SystemDistinctCode systemDistinctCode) {
    // 복잡한 비즈니스 로직
}

// 리플렉션을 통한 권한 처리
if ("ALL".equals(authorities[0])) {
    Field[] allAuthoritiesField = FieldUtils.getAllFields(Authorities.class);
    grantedAuthorities = Arrays.stream(allAuthoritiesField)
        .map(authority -> new SimpleGrantedAuthority(authority.getName()))
        .collect(Collectors.toList());
}
```

#### Book-Network - BeansConfig
```java
// 표준 Spring Security 설정
@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
}
```

### 2.3 JWT 서비스

| 기능 | SSGD_API_REAL | Book-Network |
|------|---------------|--------------|
| **토큰 생성** | 외부 TokenUtility 사용 | 자체 JwtService 구현 |
| **토큰 저장** | Redis 캐싱 | 클라이언트 측 저장만 |
| **토큰 검증** | JWT + Redis 세션 대조 | JWT 자체 검증만 |
| **만료 처리** | 자동 세션 연장 | 단순 만료 확인 |

---

## 3. 권한 관리 시스템

### SSGD_API_REAL - 세밀한 권한 체계
```java
// 도메인별 권한 정의
public class Authorities {
    // 브랜드 관련
    public static final String BRAND_MANAGER_R = "BRAND_MANAGER_R";
    public static final String BRAND_MANAGER_CUD = "BRAND_MANAGER_CUD";
    
    // 커뮤니티 관련
    public static final String COMMUNITY_R = "COMMUNITY_R";
    public static final String COMMUNITY_CUD = "COMMUNITY_CUD";
    // ... 100+ 권한 상수
}

// 복합 권한 검사
AttributeAuthorityUtility.checkAnyof(
    AuthorityCheckVO.builder()
        .allowedAuthority(Authorities.BRAND_MANAGER_R)
        .checkMyBrandCodeEqualTo(requestBrandCode)
        .build()
);
```

**특징:**
- R(Read), CUD(Create/Update/Delete) 패턴
- 도메인별 권한 세분화
- 브랜드/상점/샵 레벨 권한 제어
- 복합 조건 권한 검사

### Book-Network - 단순한 역할 기반
```java
// 기본적인 Spring Security 권한
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return roleNames.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
}
```

**특징:**
- 단순한 ROLE 기반 권한
- 기본적인 USER/ADMIN 구분

---

## 4. 세션 및 상태 관리

### SSGD_API_REAL - 하이브리드 방식
```java
// Redis 기반 세션 정보
@Data
@Builder
public class SessionUserInfo {
    private String sessionId;
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String brandCode;
    private String storeCode;
    private SystemDistinctCode systemDistinctCode;
    private List<String> appServerServiceCodeNameList;
    // ... 20+ 필드
}

// 토큰 검증 시 Redis 확인
String sessionJson = cacheRepository.retrieve(
    RedisKeyConstants.LOGIN.concat(claims.getSubject()));
if (!jwtToken.equals(sessionUserInfo.getAccessToken())) {
    throw new RestApiException("Unauthorized", HttpStatus.CONFLICT);
}
```

### Book-Network - 순수 Stateless
```java
// JWT만으로 완전한 무상태 인증
public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
}
```

---

## 5. 보안 설정 및 CORS

### SSGD_API_REAL
```java
// 프로덕션급 CORS 설정
@Bean
UrlBasedCorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowedHeaders(List.of(
        "Content-Type", "Authorization", "Session-Key", "User-Id",
        "x-id-token", "x-api-key", "mock-login", "mock-user-id"
    ));
    // 상세한 설정...
}

// 세분화된 공개 API 경로
public class SecurityConfigPublicApi {
    public static final String[] publicOpenApiGet = {
        "/swagger-ui/**", "/v3/api-docs",
        "/mnl/bo-user-login/v1/bo-users/login/sso",
        "/healthy", "/actuator/**"
    };
}
```

### Book-Network
```java
// 기본적인 보안 설정
http.cors(withDefaults())
    .csrf(AbstractHttpConfigurer::disable)
    .authorizeRequests(req ->
        req.requestMatchers("/auth/**", "/swagger-ui/**").permitAll()
           .anyRequest().authenticated()
    )
    .sessionManagement(session -> 
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
```

---

## 6. 개발 환경 지원

### SSGD_API_REAL - 엔터프라이즈 개발 지원
```java
// 프로필 기반 Mock 로그인
if (!"prd".equals(profile) && "Y".equals(mockLogin)) {
    // Mock 헤더를 통한 다양한 사용자 시뮬레이션
    String mockUserId = request.getHeader("mock-user-id");
    String mockAuthorities = request.getHeader("mock-authorities");
    String mockBrandCode = request.getHeader("mock-brandCode");
    // ... 20+ Mock 헤더 지원
}
```

### Book-Network - 기본 개발 지원
```java
// 단순한 Mock 로그인
if ("dev".equals(profile) && "Y".equals(mockLogin)) {
    UserDetails mockUser = User.builder()
        .username(userEmail)
        .password("")
        .roles(userRole)
        .build();
}
```

---

## 7. 수준 비교 및 평가

### 🏆 SSGD_API_REAL의 장점
1. **엔터프라이즈급 보안**
   - 이중 검증 시스템 (JWT + Redis)
   - 토큰 탈취 방지 메커니즘
   - 분산 환경 지원

2. **세밀한 권한 제어**
   - 100+ 권한 상수 정의
   - 도메인별/기능별 권한 세분화
   - 브랜드/상점 레벨 권한 제어

3. **확장성 및 유연성**
   - 다중 시스템 지원 (BO/PO/FO)
   - 프로필 기반 환경 설정
   - 포괄적인 Mock 시스템

4. **운영 친화적**
   - 상세한 로깅 및 예외 처리
   - Redis 기반 세션 관리
   - 헬스체크 및 모니터링 지원

### 📚 Book-Network의 장점
1. **단순성과 명확성**
   - 학습하기 쉬운 구조
   - 표준 Spring Security 패턴
   - 최소한의 의존성

2. **성능 최적화**
   - 완전한 Stateless 방식
   - 외부 저장소 의존성 없음
   - 빠른 토큰 검증

3. **표준 준수**
   - Spring Security 모범 사례
   - JWT 표준 구현
   - 코드 가독성 높음

---

## 8. Book-Network에 적용 가능한 개선점

### 8.1 즉시 적용 가능
```java
// 1. 예외 처리 개선
try {
    // JWT 검증 로직
} catch (ExpiredJwtException e) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write("Token expired");
} catch (MalformedJwtException e) {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().write("Invalid token format");
}

// 2. Mock 로그인 기능 확장
private boolean handleMockLogin(HttpServletRequest request) {
    final String mockLogin = request.getHeader("mock-login");
    final String mockRole = request.getHeader("mock-role");
    final String mockUserId = request.getHeader("mock-user-id");
    
    if ("dev".equals(profile) && "Y".equals(mockLogin)) {
        // 더 다양한 Mock 시나리오 지원
    }
}
```

### 8.2 단계적 적용 가능
```java
// 1. 권한 체계 세분화
public class BookAuthorities {
    public static final String BOOK_READ = "BOOK_READ";
    public static final String BOOK_WRITE = "BOOK_WRITE";
    public static final String USER_MANAGE = "USER_MANAGE";
    public static final String ADMIN_ALL = "ADMIN_ALL";
}

// 2. 커스텀 AuthenticationProvider 도입
@Component
public class BookAuthenticationProvider {
    public Authentication getAuthentication(UserDetails userDetails, 
                                          List<String> authorities) {
        // SSGD 스타일의 인증 객체 생성
    }
}
```

### 8.3 고급 기능 도입
```java
// 1. Redis 세션 관리 (선택적)
@Service
public class SessionService {
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void storeSession(String tokenId, UserSessionInfo sessionInfo) {
        // 세션 정보 저장
    }
    
    public boolean validateTokenWithSession(String token) {
        // 토큰과 세션 정보 대조
    }
}

// 2. 다중 권한 검사 유틸리티
public class AuthorityUtils {
    public static boolean hasAnyAuthority(String... authorities) {
        // 복합 권한 검사 로직
    }
}
```

---

## 9. 결론 및 권장사항

### 현재 상황 평가
- **SSGD_API_REAL**: ⭐⭐⭐⭐⭐ (엔터프라이즈급)
- **Book-Network**: ⭐⭐⭐ (교육/중급용)

### Book-Network 개선 로드맵

#### Phase 1: 기본 강화 (1-2주)
- [ ] 예외 처리 개선
- [ ] Mock 로그인 기능 확장
- [ ] 로깅 시스템 추가

#### Phase 2: 보안 강화 (2-3주)
- [ ] 커스텀 AuthenticationProvider 도입
- [ ] 권한 체계 세분화
- [ ] CORS 설정 상세화

#### Phase 3: 엔터프라이즈 기능 (1-2개월)
- [ ] Redis 세션 관리 도입
- [ ] 이중 검증 시스템 구현
- [ ] 모니터링 및 메트릭 추가

### 최종 평가
SSGD_API_REAL의 인증 시스템은 **대규모 프로덕션 환경**에 적합한 엔터프라이즈급 보안 아키텍처를 제공합니다. Book-Network는 **학습 및 중소규모 프로젝트**에 적합한 명확하고 표준적인 구조를 가지고 있습니다.

두 시스템 모두 각각의 목적에 맞게 잘 설계되어 있으며, SSGD의 고급 기능들을 단계적으로 Book-Network에 적용하면 점진적인 시스템 발전이 가능합니다.