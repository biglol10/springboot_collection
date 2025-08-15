# Book-Network 엔터프라이즈급 보안 시스템 구현 가이드

## 📋 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [보안 아키텍처 설계](#보안-아키텍처-설계)
3. [핵심 컴포넌트 상세 분석](#핵심-컴포넌트-상세-분석)
4. [구현된 파일별 상세 설명](#구현된-파일별-상세-설명)
5. [인증/인가 플로우](#인증인가-플로우)
6. [테스트 및 검증](#테스트-및-검증)
7. [개발자 가이드](#개발자-가이드)

---

## 🎯 프로젝트 개요

### 목표
기존 FastCampus 교육용 Book-Network 프로젝트를 **SSGD_API_REAL 수준의 엔터프라이즈급 보안 시스템**으로 업그레이드

### 변화 요약
- **이전**: ⭐⭐⭐ 기본 Spring Security + JWT (교육용)
- **현재**: ⭐⭐⭐⭐⭐ 엔터프라이즈급 다층 보안 시스템

### 주요 개선 사항
1. **커스텀 인증 제공자** - 다양한 인증 시나리오 지원
2. **세분화된 권한 체계** - 25개 권한, 4개 역할 그룹
3. **강화된 JWT 시스템** - 다층 검증, IP 추적, 리치 클레임
4. **완벽한 예외 처리** - 50+ 에러 코드, 표준화된 응답
5. **보안 응답 강화** - 보안 헤더, 추적 시스템

---

## 🏗️ 보안 아키텍처 설계

### 전체 아키텍처 다이어그램
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client Request│───▶│   JwtFilter     │───▶│  SecurityConfig │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  JwtService     │◀───│CustomAuthProvider│───▶│ AuthorityUtils  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Token Validation│    │  Authentication │    │ Authorization   │
│ - 서명 검증      │    │  - 사용자 확인   │    │ - 권한 검사     │
│ - 만료 확인      │    │  - Mock 로그인   │    │ - 역할 검증     │
│ - IP 검증       │    │  - 토큰 생성     │    │ - 리소스 접근   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Exception Handling                           │
│  SecurityExceptionHandler + 50+ Error Codes                    │
└─────────────────────────────────────────────────────────────────┘
```

### 보안 계층 구조
```
Layer 1: Request Filter (JwtFilter)
├── IP 주소 추출 및 검증
├── JWT 토큰 파싱 및 기본 검증
└── Mock 로그인 처리 (개발환경)

Layer 2: Authentication (CustomAuthenticationProvider)
├── 사용자 정보 검증
├── 권한 정보 추출 및 변환
└── Authentication 객체 생성

Layer 3: Authorization (AuthorityUtils + BookAuthorities)
├── 권한 체계 관리 (25개 세분화 권한)
├── 역할 기반 권한 그룹 (4개 역할)
└── 복합 권한 검사 로직

Layer 4: Token Management (JwtService)
├── 토큰 생성 (25개 클레임 지원)
├── 다층 검증 (서명, 만료, 발행자, IP)
└── 토큰 정보 추출 및 분석

Layer 5: Exception Handling (SecurityExceptionHandler)
├── 계층화된 예외 클래스
├── 표준화된 에러 응답
└── 상세한 로깅 및 추적
```

---

## 🔧 핵심 컴포넌트 상세 분석

### 1. JWT 기반 인증 시스템의 원리

#### 🔑 JWT (JSON Web Token) 개념
JWT는 JSON 객체를 안전하게 전송하기 위한 컴팩트하고 자체적으로 포함된 방식입니다.

**구조**: `Header.Payload.Signature`
```json
// Header (토큰 타입 및 알고리즘)
{
  "alg": "HS256",
  "typ": "JWT"
}

// Payload (클레임 - 사용자 정보)
{
  "sub": "user@example.com",
  "authorities": ["BOOK_READ", "BOOK_WRITE"],
  "iat": 1516239022,
  "exp": 1516242622,
  "jti": "token-id-123"
}

// Signature (무결성 검증)
HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

#### 🔄 JWT 인증 플로우
```
1. 사용자 로그인 요청
   ↓
2. 서버에서 사용자 검증
   ↓
3. JWT 토큰 생성 (사용자 정보 + 권한 포함)
   ↓
4. 클라이언트에게 토큰 반환
   ↓
5. 클라이언트가 요청 시 Authorization 헤더에 토큰 포함
   ↓
6. 서버에서 토큰 검증 (서명, 만료, 클레임)
   ↓
7. 검증 성공 시 요청 처리
```

### 2. Spring Security 필터 체인의 동작 원리

#### 🔗 필터 체인 개념
Spring Security는 **Filter Chain Pattern**을 사용하여 보안을 구현합니다.

```
HTTP Request
     ↓
SecurityFilterChain:
├── CorsFilter
├── CsrfFilter (우리는 비활성화)
├── JwtFilter (우리가 추가한 커스텀 필터) ← 핵심!
├── UsernamePasswordAuthenticationFilter
├── AuthorizationFilter
└── ExceptionTranslationFilter
     ↓
Controller
```

#### 🎯 JwtFilter의 역할과 중요성
```java
// 필터가 하는 일:
1. 모든 HTTP 요청을 가로챔
2. Authorization 헤더에서 JWT 토큰 추출
3. 토큰 유효성 검증
4. 사용자 정보 추출 및 SecurityContext 설정
5. 다음 필터로 요청 전달
```

**왜 UsernamePasswordAuthenticationFilter 앞에 위치하나?**
- JWT는 이미 인증된 토큰이므로 username/password 인증보다 먼저 처리
- 토큰이 유효하면 더 이상의 인증 과정 불필요

### 3. 권한 체계 (RBAC vs ABAC)

#### 📊 권한 모델 비교
```
기존 (단순 RBAC):
User → Role (USER/ADMIN) → Permissions

우리 시스템 (고급 RBAC + ABAC):
User → Role (USER/BOOK_MANAGER/SYSTEM_ADMIN/SUPER_ADMIN) 
     → Authorities (25개 세분화) 
     → Context (Brand/Store/IP 등)
```

#### 🏷️ 권한 명명 규칙
```
{DOMAIN}_{ACTION} 패턴:
- BOOK_READ: 도서 조회
- BOOK_CUD: 도서 생성/수정/삭제  
- USER_MANAGE: 사용자 관리
- TRANSACTION_BORROW: 도서 대출
- SYSTEM_MANAGE: 시스템 관리
```

### 4. 예외 처리 아키텍처

#### 🎯 계층화된 예외 설계
```
SecurityException (최상위)
├── TokenException (토큰 관련)
│   ├── TOKEN_EXPIRED
│   ├── TOKEN_INVALID
│   └── TOKEN_IP_MISMATCH
├── AuthenticationException (인증 관련)
│   ├── USER_NOT_FOUND
│   ├── INVALID_CREDENTIALS
│   └── AUTHENTICATION_FAILED
└── AuthorizationException (인가 관련)
    ├── ACCESS_DENIED
    ├── INSUFFICIENT_PRIVILEGES
    └── ROLE_REQUIRED
```

#### 🔄 예외 처리 플로우
```
예외 발생 → SecurityExceptionHandler 
→ 예외 타입별 처리 
→ SecurityErrorResponse 생성 
→ 클라이언트에게 표준화된 응답
```

---

## 📁 구현된 파일별 상세 설명

### 🔐 인증 관련 파일들

#### 1. `CustomAuthenticationProvider.java`
```java
@Component
public class CustomAuthenticationProvider {
    // 핵심 역할: 사용자 정보 + 권한 → Authentication 객체 생성
    
    // 💡 주요 메서드들:
    
    // 1. 일반 인증 (JWT에서 추출한 정보 기반)
    public Authentication getAuthentication(UserDetails userDetails, List<String> authorities)
    
    // 2. Mock 로그인 (개발환경용)
    public Authentication getMockAuthentication(String username, String[] authorities, ...)
    
    // 3. 특수 권한 처리 (ALL, SUPER_ADMIN 등)
    public Authentication getSpecialAuthentication(UserDetails userDetails, String authorityType)
    
    // 4. 역할 기반 인증
    public Authentication getRoleBasedAuthentication(UserDetails userDetails, String role)
}
```

**개념과 의의:**
- **책임 분리**: 인증 로직을 별도 클래스로 분리하여 유지보수성 향상
- **확장성**: 다양한 인증 시나리오를 메서드별로 처리
- **테스트 용이성**: Mock 로그인을 통한 개발 환경 지원

#### 2. `CustomAuthenticationToken.java`
```java
public class CustomAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal;
    
    // 💡 핵심 포인트:
    @Override
    public Object getCredentials() {
        return null; // JWT 방식에서는 자격 증명 저장 불필요
    }
    
    @Override
    public Object getPrincipal() {
        return this.principal; // UserDetails 또는 커스텀 Principal
    }
}
```

**개념과 의의:**
- **JWT 특화**: 비밀번호 기반 인증과 달리 자격 증명(credentials) 불필요
- **타입 안전성**: Spring Security와 완벽 호환되는 Authentication 구현체
- **확장 가능성**: 필요시 추가 정보를 포함할 수 있는 구조

#### 3. `MockUserDetails.java`
```java
@Builder
public class MockUserDetails implements UserDetails {
    private final Integer id;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    
    // 💡 Mock 전용 UserDetails
    // 실제 DB 조회 없이 테스트용 사용자 정보 제공
}
```

**개념과 의의:**
- **개발 효율성**: DB 없이도 다양한 사용자 시나리오 테스트 가능
- **격리된 테스트**: 외부 의존성 없는 보안 로직 테스트
- **빠른 개발**: 인증 로직 개발 시 빠른 피드백

### 🛡️ JWT 관련 파일들

#### 4. `JwtService.java` (대폭 강화)
```java
@Service
public class JwtService {
    // 💡 엔터프라이즈급 기능들:
    
    // 1. 토큰 생성 (25개 클레임 지원)
    public String generateEnhancedToken(Map<String, Object> extraClaims, 
                                      UserDetails userDetails, 
                                      String ipAddress, String userAgent, String deviceId)
    
    // 2. 다층 보안 검증
    public boolean isTokenValid(String token, UserDetails userDetails) {
        // 검증 단계:
        // - 기본 구조 및 서명 검증
        // - 사용자명 일치 확인
        // - 토큰 만료 확인
        // - 토큰 타입 확인 (ACCESS/REFRESH)
        // - 발행자 및 대상자 검증
        // - 유효 시작 시간 확인 (nbf)
    }
    
    // 3. IP 주소 기반 검증
    public boolean isTokenValidWithIpCheck(String token, UserDetails userDetails, String clientIpAddress)
    
    // 4. 리치 클레임 추출
    public String extractTokenId(String token)
    public Integer extractUserId(String token)
    public List<String> extractAuthorities(String token)
    public String extractIpAddress(String token)
}
```

**토큰 생성 과정 상세:**
```java
// 1. 표준 클레임 설정
.setId(tokenId)           // JWT ID (jti) - 토큰 추적용
.setIssuer(issuer)        // 발행자 - "book-network"
.setAudience(audience)    // 대상자 - "book-network-users"
.setSubject(username)     // 주체 - 사용자 이메일
.setIssuedAt(now)         // 발행 시간
.setExpiration(expiryDate) // 만료 시간
.setNotBefore(now)        // 유효 시작 시간

// 2. 커스텀 클레임 설정
.claim("authorities", authorities)  // 권한 목록
.claim("tokenType", "ACCESS")       // 토큰 타입
.claim("userId", userId)            // 사용자 ID
.claim("ipAddress", ipAddress)      // 발급 시 IP
.claim("userAgent", userAgent)      // User-Agent 정보
```

**다층 검증의 의의:**
- **보안 강화**: 단순 서명 검증을 넘어 다각도 검증
- **위조 방지**: IP, User-Agent 등 컨텍스트 정보로 토큰 도용 방지
- **감사 추적**: 토큰 ID를 통한 사용 이력 추적 가능

#### 5. `JwtFilter.java` (강화)
```java
public class JwtFilter extends OncePerRequestFilter {
    // 💡 필터 동작 플로우:
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        
        // 1. 인증 불필요 경로 체크
        if (request.getServletPath().contains("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 2. Mock 로그인 처리 (개발환경)
        if (handleMockLogin(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 3. JWT 토큰 인증 처리
        if (handleJwtAuthentication(request)) {
            // 인증 성공
        }
        
        // 4. 다음 필터로 전달
        filterChain.doFilter(request, response);
    }
    
    // 💡 IP 주소 추출 (프록시/로드밸런서 지원)
    private String getClientIpAddress(HttpServletRequest request) {
        // 우선순위: X-Forwarded-For → X-Real-IP → Proxy-Client-IP → RemoteAddr
    }
}
```

**IP 주소 추출의 중요성:**
- **프록시 환경 지원**: 로드밸런서 뒤에서도 실제 클라이언트 IP 추출
- **보안 강화**: IP 기반 토큰 검증으로 토큰 도용 방지
- **위치 추적**: 비정상적인 접근 위치 감지 가능

### 🔑 권한 관리 파일들

#### 6. `BookAuthorities.java`
```java
public class BookAuthorities {
    // 💡 권한 체계 설계:
    
    // 1. 도메인별 권한 (25개)
    public static final String BOOK_READ = "BOOK_READ";
    public static final String BOOK_CUD = "BOOK_CUD";
    public static final String USER_MANAGE = "USER_MANAGE";
    // ... 22개 더
    
    // 2. 역할별 권한 그룹 (4개)
    public static final String[] USER_BASIC_AUTHORITIES = {
        BOOK_READ, FEEDBACK_READ, FEEDBACK_WRITE, 
        TRANSACTION_READ, TRANSACTION_BORROW, TRANSACTION_RETURN
    };
    
    public static final String[] BOOK_MANAGER_AUTHORITIES = {
        BOOK_READ, BOOK_CUD, BOOK_SHARE_MANAGE, BOOK_FILE_MANAGE,
        FEEDBACK_READ, FEEDBACK_MANAGE, TRANSACTION_READ, TRANSACTION_MANAGE
    };
    
    // 3. 유틸리티 메서드
    public static String[] getAuthoritiesByRole(String role) {
        // 역할에 따른 권한 목록 반환
    }
    
    public static String[] getAllAuthorities() {
        // 모든 권한 반환 (SSGD의 "ALL" 권한과 동일)
    }
}
```

**권한 설계 철학:**
- **최소 권한 원칙**: 필요한 최소한의 권한만 부여
- **역할 기반 그룹핑**: 직무별로 권한을 묶어 관리 편의성 제공
- **확장성**: 새로운 기능 추가 시 권한 체계 쉽게 확장 가능

#### 7. `AuthorityUtils.java`
```java
public class AuthorityUtils {
    // 💡 권한 검사 유틸리티:
    
    // 1. 기본 권한 검사
    public static boolean hasAuthority(String authority)
    public static boolean hasAnyAuthority(String... authorities)
    public static boolean hasAllAuthorities(String... authorities)
    
    // 2. 역할 기반 검사
    public static boolean hasRole(String role)
    public static boolean hasAnyRole(String... roles)
    
    // 3. 비즈니스 로직 기반 검사
    public static boolean canAccessBooks()
    public static boolean canModifyBooks()
    public static boolean canManageUsers()
    public static boolean isAdmin()
    
    // 4. 복합 권한 검사 (SSGD 스타일)
    public static boolean checkComplexAuthority(String[] requiredAuthorities, 
                                              String[] optionalAuthorities)
}
```

**유틸리티 클래스의 의의:**
- **편의성**: 복잡한 권한 검사 로직을 간단한 메서드 호출로 해결
- **일관성**: 애플리케이션 전체에서 동일한 권한 검사 방식 사용
- **유지보수성**: 권한 검사 로직 변경 시 한 곳에서만 수정

### ⚠️ 예외 처리 파일들

#### 8. `SecurityException.java` (기본 예외)
```java
public class SecurityException extends RuntimeException {
    private final String errorCode;
    private final Object[] args;
    
    // 💡 설계 특징:
    // - 에러 코드 기반 분류
    // - 파라미터화된 메시지 지원
    // - 국제화(i18n) 준비된 구조
}
```

#### 9. `TokenException.java` (토큰 예외)
```java
public class TokenException extends SecurityException {
    // 💡 토큰 관련 에러 코드 (13개)
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String TOKEN_IP_MISMATCH = "TOKEN_IP_MISMATCH";
    // ... 11개 더
    
    // 편의 메서드들
    public static TokenException expired(String message)
    public static TokenException ipMismatch(String tokenIp, String requestIp)
}
```

#### 10. `SecurityExceptionHandler.java` (글로벌 예외 처리)
```java
@RestControllerAdvice
public class SecurityExceptionHandler {
    // 💡 예외별 핸들러:
    
    @ExceptionHandler(TokenException.class)
    public ResponseEntity<SecurityErrorResponse> handleTokenException(...)
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<SecurityErrorResponse> handleAuthenticationException(...)
    
    @ExceptionHandler(ExpiredJwtException.class)  // JWT 라이브러리 예외
    public ResponseEntity<SecurityErrorResponse> handleExpiredJwtException(...)
}
```

**글로벌 예외 처리의 의의:**
- **일관된 응답**: 모든 예외에 대해 동일한 형식의 응답 제공
- **자동 처리**: 개발자가 각 메서드마다 try-catch 작성할 필요 없음
- **로깅 통합**: 모든 예외를 한 곳에서 로깅하여 모니터링 용이

#### 11. `SecurityErrorResponse.java` (표준 에러 응답)
```java
@Data @Builder
public class SecurityErrorResponse {
    private int status;           // HTTP 상태 코드
    private String errorCode;     // 비즈니스 에러 코드
    private String message;       // 사용자용 메시지
    private String details;       // 개발자용 상세 정보
    private LocalDateTime timestamp; // 에러 발생 시각
    private String path;          // 에러 발생 경로
    private String traceId;       // 추적 ID
    private String category;      // 에러 카테고리
    private Boolean retryable;    // 재시도 가능 여부
    
    // 💡 팩토리 메서드들:
    public static SecurityErrorResponse tokenError(...)
    public static SecurityErrorResponse authenticationError(...)
    public static SecurityErrorResponse authorizationError(...)
}
```

### 🎛️ 설정 및 보안 강화 파일들

#### 12. `SecurityResponseEnhancer.java` (응답 보안 강화)
```java
@RestControllerAdvice
public class SecurityResponseEnhancer implements ResponseBodyAdvice<Object> {
    // 💡 모든 API 응답에 보안 헤더 추가:
    
    private void addSecurityHeaders(ServerHttpResponse response) {
        // CORS 보안
        response.getHeaders().add("X-Content-Type-Options", "nosniff");
        response.getHeaders().add("X-Frame-Options", "DENY");
        response.getHeaders().add("X-XSS-Protection", "1; mode=block");
        
        // CSP 헤더
        response.getHeaders().add("Content-Security-Policy", "default-src 'none'");
        
        // 캐시 제어
        response.getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
        
        // HSTS
        response.getHeaders().add("Strict-Transport-Security", "max-age=31536000");
    }
}
```

**보안 헤더의 중요성:**
- **XSS 방지**: X-XSS-Protection으로 브라우저 XSS 필터 활성화
- **Clickjacking 방지**: X-Frame-Options로 iframe 삽입 차단
- **MIME 스니핑 방지**: X-Content-Type-Options로 MIME 타입 강제
- **HTTPS 강제**: HSTS로 HTTP → HTTPS 자동 리다이렉트

#### 13. `SecurityTestController.java` (테스트 컨트롤러)
```java
@RestController
@RequestMapping("/api/v1/security-test")
public class SecurityTestController {
    // 💡 포괄적인 테스트 엔드포인트:
    
    @GetMapping("/current-user")           // 현재 사용자 정보
    @GetMapping("/check-authorities")      // 권한 확인
    @GetMapping("/jwt-info")              // JWT 토큰 분석
    @GetMapping("/validate-token")        // 토큰 검증
    @GetMapping("/test-exception/{type}") // 예외 처리 테스트 (35+ 종류)
}
```

---

## 🔄 인증/인가 플로우

### 전체 플로우 다이어그램
```
┌─────────────────┐
│  1. HTTP Request│
│  with JWT Token │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  2. JwtFilter   │
│  ┌─────────────┐│
│  │ IP 추출     ││
│  │ 토큰 추출   ││
│  │ Mock 체크   ││
│  └─────────────┘│
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  3. JwtService  │
│  ┌─────────────┐│
│  │ 서명 검증   ││
│  │ 만료 확인   ││
│  │ IP 검증     ││
│  │ 클레임 추출 ││
│  └─────────────┘│
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│4. UserDetailsService│
│  ┌─────────────┐│
│  │ 사용자 조회 ││
│  │ UserDetails ││
│  │ 반환        ││
│  └─────────────┘│
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│5. CustomAuthProvider│
│  ┌─────────────┐│
│  │ 권한 변환   ││
│  │ Authentication││
│  │ 객체 생성   ││
│  └─────────────┘│
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│6. SecurityContext│
│  ┌─────────────┐│
│  │ 인증 정보   ││
│  │ 저장        ││
│  └─────────────┘│
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│7. Controller    │
│  ┌─────────────┐│
│  │ 비즈니스    ││
│  │ 로직 실행   ││
│  └─────────────┘│
└─────────────────┘
```

### 단계별 상세 설명

#### 1단계: 요청 수신 및 필터링
```java
// JwtFilter.doFilterInternal()
1. HTTP 요청 수신
2. 인증 불필요 경로 체크 (/auth/**)
3. Mock 로그인 헤더 확인 (개발환경)
4. Authorization 헤더에서 JWT 토큰 추출
5. 토큰 형식 검증 ("Bearer " 접두사)
```

#### 2단계: JWT 토큰 검증
```java
// JwtService.isTokenValid()
1. 토큰 파싱 및 클레임 추출
2. 서명 검증 (SecretKey 사용)
3. 만료 시간 확인
4. 발행자(issuer) 검증
5. 토큰 타입 확인 (ACCESS/REFRESH)
6. 유효 시작 시간(nbf) 확인
7. IP 주소 매칭 (선택적)
```

#### 3단계: 사용자 정보 로드
```java
// UserDetailsService.loadUserByUsername()
1. JWT에서 사용자명(이메일) 추출
2. 데이터베이스에서 사용자 정보 조회
3. UserDetails 객체 생성 및 반환
4. 사용자 존재하지 않으면 예외 발생
```

#### 4단계: 인증 객체 생성
```java
// CustomAuthenticationProvider.getAuthentication()
1. JWT에서 권한 목록 추출
2. 권한 문자열을 GrantedAuthority 객체로 변환
3. CustomAuthenticationToken 생성
4. 요청 상세 정보 설정 (IP, User-Agent 등)
```

#### 5단계: 보안 컨텍스트 설정
```java
// SecurityContextHolder
1. Authentication 객체를 SecurityContext에 저장
2. 현재 스레드에서 인증 정보 접근 가능
3. 후속 필터 및 컨트롤러에서 사용자 정보 활용
```

### Mock 로그인 플로우 (개발환경)
```
개발환경 요청 (mock-login: Y 헤더)
          │
          ▼
┌─────────────────┐
│ Mock 헤더 추출  │
│ - mock-login-email
│ - mock-authorities
│ - mock-role
│ - mock-user-id
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│MockUserDetails  │
│객체 생성        │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│CustomAuthentication│
│Token 생성       │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│SecurityContext  │
│설정 완료        │
└─────────────────┘
```

---

## 🧪 테스트 및 검증

### 1. Mock 로그인 테스트

#### 기본 Mock 로그인
```bash
curl -X GET "http://localhost:8080/api/v1/security-test/current-user" \
  -H "mock-login: Y" \
  -H "mock-login-email: test@example.com"
```

#### 고급 Mock 로그인 (모든 헤더)
```bash
curl -X GET "http://localhost:8080/api/v1/security-test/current-user" \
  -H "mock-login: Y" \
  -H "mock-login-email: admin@example.com" \
  -H "mock-user-id: 1" \
  -H "mock-role: ADMIN" \
  -H "mock-authorities: ADMIN_ALL" \
  -H "mock-first-name: Admin" \
  -H "mock-last-name: User"
```

#### 역할별 Mock 테스트
```bash
# 일반 사용자
curl -H "mock-login: Y" -H "mock-role: USER" \
  "http://localhost:8080/api/v1/security-test/check-authorities"

# 도서 관리자  
curl -H "mock-login: Y" -H "mock-role: BOOK_MANAGER" \
  "http://localhost:8080/api/v1/security-test/check-authorities"

# 시스템 관리자
curl -H "mock-login: Y" -H "mock-role: SYSTEM_ADMIN" \
  "http://localhost:8080/api/v1/security-test/check-authorities"

# 최고 관리자
curl -H "mock-login: Y" -H "mock-authorities: ADMIN_ALL" \
  "http://localhost:8080/api/v1/security-test/check-authorities"
```

### 2. JWT 토큰 분석 테스트

#### 토큰 상세 정보 조회
```bash
curl -X GET "http://localhost:8080/api/v1/security-test/jwt-info" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**예상 응답:**
```json
{
  "tokenInfo": {
    "tokenId": "abc123...",
    "subject": "user@example.com",
    "issuer": "book-network",
    "audience": "book-network-users",
    "issuedAt": "2024-01-15T10:30:00",
    "expiration": "2024-01-15T11:30:00",
    "authorities": ["BOOK_READ", "BOOK_WRITE"],
    "userId": 123,
    "tokenType": "ACCESS",
    "ipAddress": "192.168.1.100"
  },
  "isExpired": false,
  "isNearExpiry": false,
  "clientIP": "192.168.1.100"
}
```

#### 토큰 검증 테스트
```bash
curl -X GET "http://localhost:8080/api/v1/security-test/validate-token" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. 예외 처리 테스트 (35+ 종류)

#### 토큰 관련 예외 (9종류)
```bash
# 토큰 만료
curl "http://localhost:8080/api/v1/security-test/test-exception/token/expired"

# 토큰 형식 오류
curl "http://localhost:8080/api/v1/security-test/test-exception/token/malformed"

# IP 주소 불일치
curl "http://localhost:8080/api/v1/security-test/test-exception/token/ipmismatch"

# 토큰 타입 오류
curl "http://localhost:8080/api/v1/security-test/test-exception/token/wrongtype"
```

#### 인증 관련 예외 (10종류)
```bash
# 사용자 없음
curl "http://localhost:8080/api/v1/security-test/test-exception/auth/usernotfound"

# 계정 비활성화
curl "http://localhost:8080/api/v1/security-test/test-exception/auth/userdisabled"

# 자격 증명 만료
curl "http://localhost:8080/api/v1/security-test/test-exception/auth/credentialsexpired"
```

#### 권한 관련 예외 (12종류)
```bash
# 접근 거부
curl "http://localhost:8080/api/v1/security-test/test-exception/authz/accessdenied"

# 권한 부족
curl "http://localhost:8080/api/v1/security-test/test-exception/authz/insufficient"

# 관리자 권한 필요
curl "http://localhost:8080/api/v1/security-test/test-exception/authz/adminrequired"
```

#### 예외 테스트 목록 조회
```bash
curl "http://localhost:8080/api/v1/security-test/test-exception/list"
```

### 4. 권한 체계 테스트

#### 역할별 권한 확인
```bash
curl "http://localhost:8080/api/v1/security-test/test-roles"
```

**예상 응답:**
```json
{
  "USER_AUTHORITIES": ["BOOK_READ", "FEEDBACK_READ", "FEEDBACK_WRITE", "TRANSACTION_READ", "TRANSACTION_BORROW", "TRANSACTION_RETURN"],
  "BOOK_MANAGER_AUTHORITIES": ["BOOK_READ", "BOOK_CUD", "BOOK_SHARE_MANAGE", "BOOK_FILE_MANAGE", "FEEDBACK_READ", "FEEDBACK_MANAGE", "TRANSACTION_READ", "TRANSACTION_MANAGE"],
  "SYSTEM_ADMIN_AUTHORITIES": ["USER_READ", "USER_MANAGE", "SYSTEM_READ", "SYSTEM_MANAGE", "SYSTEM_LOG_READ", "ADMIN_BASIC", "ADMIN_ADVANCED"],
  "SUPER_ADMIN_AUTHORITIES": ["ADMIN_ALL"],
  "ALL_AUTHORITIES": ["BOOK_READ", "BOOK_CUD", ... (25개 모든 권한)]
}
```

### 5. 보안 헤더 확인

#### 응답 헤더 검증
```bash
curl -I "http://localhost:8080/api/v1/security-test/current-user" \
  -H "mock-login: Y"
```

**예상 헤더들:**
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
Cache-Control: no-cache, no-store, must-revalidate
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Request-ID: abc12345
X-API-Version: 1.0
X-Security-Policy-Version: 2024.1
```

---

## 👨‍💻 개발자 가이드

### 새로운 권한 추가하기

#### 1단계: BookAuthorities에 권한 추가
```java
public class BookAuthorities {
    // 새 권한 추가
    public static final String NEW_FEATURE_READ = "NEW_FEATURE_READ";
    public static final String NEW_FEATURE_WRITE = "NEW_FEATURE_WRITE";
    
    // 역할별 권한 그룹에도 추가
    public static final String[] BOOK_MANAGER_AUTHORITIES = {
        BOOK_READ, BOOK_CUD, BOOK_SHARE_MANAGE, BOOK_FILE_MANAGE,
        NEW_FEATURE_READ, NEW_FEATURE_WRITE, // 추가
        FEEDBACK_READ, FEEDBACK_MANAGE, TRANSACTION_READ, TRANSACTION_MANAGE
    };
}
```

#### 2단계: AuthorityUtils에 편의 메서드 추가
```java
public class AuthorityUtils {
    public static boolean canAccessNewFeature() {
        return hasAnyAuthority(
            BookAuthorities.NEW_FEATURE_READ,
            BookAuthorities.NEW_FEATURE_WRITE,
            BookAuthorities.ADMIN_ALL
        );
    }
}
```

#### 3단계: 컨트롤러에서 권한 검사
```java
@RestController
public class NewFeatureController {
    
    @GetMapping("/new-feature")
    public ResponseEntity<?> getNewFeature() {
        if (!AuthorityUtils.canAccessNewFeature()) {
            throw AuthorizationException.authorityRequired(BookAuthorities.NEW_FEATURE_READ);
        }
        // 비즈니스 로직
    }
    
    // 또는 애노테이션 방식
    @GetMapping("/new-feature-annotated")
    @PreAuthorize("hasAuthority('NEW_FEATURE_READ')")
    public ResponseEntity<?> getNewFeatureAnnotated() {
        // 비즈니스 로직
    }
}
```

### 새로운 예외 타입 추가하기

#### 1단계: 예외 클래스에 에러 코드 추가
```java
public class AuthorizationException extends SecurityException {
    public static final String NEW_ERROR_TYPE = "NEW_ERROR_TYPE";
    
    public static AuthorizationException newErrorType(String details) {
        return new AuthorizationException(NEW_ERROR_TYPE, 
            "New error occurred: %s".formatted(details), details);
    }
}
```

#### 2단계: 예외 핸들러에 처리 로직 추가 (선택적)
```java
@RestControllerAdvice
public class SecurityExceptionHandler {
    // 기존 핸들러가 자동으로 처리하므로 
    // 특별한 처리가 필요한 경우만 추가
}
```

### JWT 토큰에 새로운 클레임 추가하기

#### 1단계: JwtService에 클레임 상수 추가
```java
public class JwtService {
    public static final String CLAIM_NEW_INFO = "newInfo";
    
    // buildEnhancedToken 메서드에서 클레임 추가
    if (newInfo != null) {
        builder.claim(CLAIM_NEW_INFO, newInfo);
    }
}
```

#### 2단계: 클레임 추출 메서드 추가
```java
public String extractNewInfo(String token) {
    return extractClaim(token, claims -> (String) claims.get(CLAIM_NEW_INFO));
}
```

### 커스텀 인증 로직 추가하기

#### CustomAuthenticationProvider 확장
```java
@Component
public class CustomAuthenticationProvider {
    
    public Authentication getCustomAuthentication(String customType, Object customData) {
        switch (customType) {
            case "LDAP":
                return handleLdapAuthentication(customData);
            case "OAUTH":
                return handleOAuthAuthentication(customData);
            default:
                throw AuthenticationException.authenticationFailed("Unsupported auth type: " + customType);
        }
    }
}
```

### 테스트 작성 가이드

#### 1. 단위 테스트 예시
```java
@ExtendWith(MockitoExtension.class)
class CustomAuthenticationProviderTest {
    
    @Mock
    private UserDetails userDetails;
    
    @InjectMocks
    private CustomAuthenticationProvider authProvider;
    
    @Test
    void testGetAuthentication() {
        // Given
        List<String> authorities = List.of("BOOK_READ", "BOOK_WRITE");
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());
        
        // When
        Authentication auth = authProvider.getAuthentication(userDetails, authorities);
        
        // Then
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities()).hasSize(2);
    }
}
```

#### 2. 통합 테스트 예시
```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testMockLogin() throws Exception {
        mockMvc.perform(get("/api/v1/security-test/current-user")
                .header("mock-login", "Y")
                .header("mock-login-email", "test@example.com")
                .header("mock-authorities", "BOOK_READ,BOOK_WRITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.customAuth").value(true));
    }
}
```

### 프로덕션 배포 시 주의사항

#### 1. Mock 로그인 비활성화
```java
// JwtFilter에서 프로덕션 환경 체크
if (!"prd".equals(profile) && "Y".equals(mockLogin)) {
    // Mock 로그인 처리
}
```

#### 2. 보안 설정 강화
```yaml
# application-prod.yml
application:
  security:
    jwt:
      secret-key: ${JWT_SECRET_KEY} # 환경변수에서 주입
      expiration: 1800000 # 30분
      issuer: "book-network-prod"
      audience: "book-network-prod-users"

logging:
  level:
    com.alibou.booknetwork.security: WARN # 디버그 로그 비활성화
```

#### 3. CORS 설정 제한
```java
@Bean
public UrlBasedCorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    if ("prd".equals(profile)) {
        configuration.setAllowedOrigins(List.of("https://your-domain.com")); // 특정 도메인만
    } else {
        configuration.setAllowedOriginPatterns(List.of("*")); // 개발환경에서만 전체 허용
    }
    // ... 나머지 설정
}
```

### 모니터링 및 로깅

#### 1. 보안 이벤트 로깅
```java
// 중요한 보안 이벤트만 별도 로거 사용
private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

public boolean isTokenValid(String token, UserDetails userDetails) {
    try {
        // 검증 로직
        securityLogger.info("Token validation successful for user: {}", userDetails.getUsername());
        return true;
    } catch (Exception e) {
        securityLogger.warn("Token validation failed for user: {}, reason: {}", 
                           userDetails.getUsername(), e.getMessage());
        return false;
    }
}
```

#### 2. 메트릭 수집 (향후 확장)
```java
// 토큰 검증 성공/실패 카운터
@Component
public class SecurityMetrics {
    private final Counter tokenValidationSuccess = Counter.builder("token.validation.success").register(meterRegistry);
    private final Counter tokenValidationFailure = Counter.builder("token.validation.failure").register(meterRegistry);
    
    public void recordTokenValidationSuccess() {
        tokenValidationSuccess.increment();
    }
    
    public void recordTokenValidationFailure(String reason) {
        tokenValidationFailure.increment(Tags.of("reason", reason));
    }
}
```

---

## 🎯 결론

이 문서를 통해 Book-Network 프로젝트가 **교육용 수준에서 엔터프라이즈급 보안 시스템**으로 성장한 과정을 상세히 확인할 수 있습니다.

### 핵심 성취사항:
1. **SSGD 수준의 보안 아키텍처** 구현
2. **25개 세분화된 권한 체계** 도입  
3. **다층 JWT 검증 시스템** 구축
4. **50+ 예외 타입의 체계적 처리** 구현
5. **포괄적인 테스트 시스템** 제공

이제 Book-Network는 **실제 프로덕션 환경**에서 사용할 수 있는 견고한 보안 시스템을 갖추었으며, 향후 Phase 2 (Redis 세션 관리), Phase 3 (모니터링/메트릭) 등으로 더욱 발전시킬 수 있는 확장 가능한 기반을 마련했습니다.