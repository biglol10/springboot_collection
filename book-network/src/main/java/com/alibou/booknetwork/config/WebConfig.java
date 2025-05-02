package com.alibou.booknetwork.config;

import com.alibou.booknetwork.handler.RoleCheckInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 웹 MVC 설정 클래스
 * 
 * 이 클래스는 Spring MVC 웹 애플리케이션의 다양한 설정을 담당합니다.
 * WebMvcConfigurer 인터페이스를 구현하여 인터셉터, CORS, 리소스 핸들러 등을
 * 커스터마이징할 수 있습니다.
 * 
 * 주요 기능:
 * - 역할 기반 접근 제어를 위한 인터셉터 등록
 * - CORS(Cross-Origin Resource Sharing) 설정
 * - 정적 리소스 핸들링 구성
 * - 파일 업로드 경로 설정
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RoleCheckInterceptor roleCheckInterceptor;

    /**
     * 생성자를 통한 의존성 주입
     * 
     * @param roleCheckInterceptor 역할 확인 인터셉터
     */
    public WebConfig(RoleCheckInterceptor roleCheckInterceptor) {
        this.roleCheckInterceptor = roleCheckInterceptor;
    }

    /**
     * 인터셉터 등록
     * 
     * 요청 경로 패턴에 따라 인터셉터를 적용합니다.
     * '/admin/**' 경로 패턴에 대해 roleCheckInterceptor를 적용하여
     * 관리자 권한을 가진 사용자만 접근할 수 있도록 제한합니다.
     * 
     * @param registry 인터셉터 레지스트리
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleCheckInterceptor).addPathPatterns("/admin/**");
    }

    /**
     * CORS 설정
     * 
     * Cross-Origin Resource Sharing을 구성하여 다른 도메인에서의 API 접근을 허용합니다.
     * 프론트엔드 애플리케이션이 백엔드 API에 접근할 수 있도록 설정합니다.
     * 
     * @param registry CORS 레지스트리
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "https://booknetwork.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 정적 리소스 핸들러 설정
     * 
     * 파일 업로드 디렉토리와 같은 물리적 경로를 웹에서 접근 가능한 URL로 매핑합니다.
     * 
     * @param registry 리소스 핸들러 레지스트리
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드된 파일에 대한 접근 경로 설정
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600);
        
        // Swagger UI 리소스 경로 설정 (API 문서화 사용 시)
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .resourceChain(false);
    }

    /**
     * 전역 CORS 필터 설정
     * 
     * Spring Security와 함께 사용할 때 필요한 CORS 필터를 빈으로 등록합니다.
     * 
     * @return CorsFilter 빈
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("https://booknetwork.com");
        config.addAllowedHeader("*");
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 추가 인터셉터 구현:
 *    - 요청 로깅 인터셉터: 애플리케이션의 모든 요청과 응답을 로깅하여 디버깅 및 모니터링에 활용
 *    - 성능 측정 인터셉터: 각 요청의 처리 시간을 측정하여 성능 병목 지점 식별
 *    - 사용자 활동 추적 인터셉터: 사용자의 행동 패턴 분석을 위한 데이터 수집
 *    
 *    예시:
 *    @Component
 *    public class MetricsInterceptor implements HandlerInterceptor {
 *        @Override
 *        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
 *            request.setAttribute("startTime", System.currentTimeMillis());
 *            return true;
 *        }
 *        
 *        @Override
 *        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
 *            long startTime = (Long) request.getAttribute("startTime");
 *            long executionTime = System.currentTimeMillis() - startTime;
 *            log.info("Request to {} completed in {} ms", request.getRequestURI(), executionTime);
 *        }
 *    }
 * 
 * 2. 고급 CORS 설정:
 *    - 환경별 CORS 설정 분리: 개발, 테스트, 프로덕션 환경에 따른 CORS 설정 다변화
 *    - 동적 CORS 설정: 데이터베이스나 설정 서버에서 허용된 도메인 목록을 동적으로 로드
 *    - 세분화된 엔드포인트별 CORS 정책: 경로별로 다른 CORS 정책 적용
 *    
 *    예시:
 *    @Autowired
 *    private CorsConfigurationRepository corsConfigRepository;
 *    
 *    @Override
 *    public void addCorsMappings(CorsRegistry registry) {
 *        List<CorsConfigEntry> entries = corsConfigRepository.findAll();
 *        for (CorsConfigEntry entry : entries) {
 *            registry.addMapping(entry.getPathPattern())
 *                    .allowedOrigins(entry.getAllowedOrigins().toArray(new String[0]))
 *                    .allowedMethods(entry.getAllowedMethods().toArray(new String[0]))
 *                    .maxAge(entry.getMaxAge());
 *        }
 *    }
 * 
 * 3. 컨텐츠 압축 및 캐싱 최적화:
 *    - Gzip 또는 Brotli 압축 필터 구현
 *    - 응답 캐싱 전략 적용 (ETag, Last-Modified 헤더 관리)
 *    - 정적 리소스 버전 관리 (캐시 버스팅)
 *    
 *    예시:
 *    @Bean
 *    public Filter compressionFilter() {
 *        CompressingFilter compressingFilter = new CompressingFilter();
 *        return compressingFilter;
 *    }
 * 
 * 4. API 속도 제한 적용:
 *    - 사용자/IP별 요청 속도 제한 필터 구현
 *    - 버킷 알고리즘 또는 토큰 시스템 적용
 *    - 속도 제한 초과 시 적절한 응답 제공
 *    
 *    예시:
 *    @Component
 *    public class RateLimitInterceptor implements HandlerInterceptor {
 *        private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 초당 10개 요청 허용
 *        
 *        @Override
 *        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
 *            if (!rateLimiter.tryAcquire()) {
 *                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
 *                response.setHeader("Retry-After", "1");
 *                return false;
 *            }
 *            return true;
 *        }
 *    }
 * 
 * 5. 다국어 지원 강화:
 *    - 로케일 변경 인터셉터 구현
 *    - 사용자 언어 선호도에 따른 콘텐츠 제공
 *    - 다국어 메시지 리소스 관리
 * 
 * 6. 보안 헤더 필터:
 *    - OWASP 권장 보안 헤더 적용 (CSP, HSTS, X-Content-Type-Options 등)
 *    - XSS 및 CSRF 방어 강화
 *    - 프레임 삽입 방지 헤더 설정
 * 
 * 7. API 문서화 자동화:
 *    - Swagger/OpenAPI 통합
 *    - 문서 엔드포인트에 대한 보안 설정
 *    - 버전별 API 문서 관리
 * 
 * 8. 요청 유효성 검증 확장:
 *    - 커스텀 유효성 검증기 등록
 *    - API 요청 스키마 검증 필터
 *    - 다양한 컨텐츠 타입 지원 (JSON, XML, GraphQL 등)
 */
