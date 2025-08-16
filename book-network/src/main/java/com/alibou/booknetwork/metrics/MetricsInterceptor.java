package com.alibou.booknetwork.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP 요청 메트릭 수집 인터셉터
 * 
 * 이 클래스는 모든 HTTP 요청에 대한 상세한 메트릭을 자동으로 수집합니다.
 * 
 * 주요 기능:
 * 1. 엔드포인트별 응답 시간 측정
 * 2. HTTP 상태 코드별 요청 수 집계
 * 3. 사용자별 요청 패턴 추적
 * 4. 에러율 및 성공률 계산
 * 
 * 왜 필요한가?
 * - API 성능 최적화를 위한 데이터 수집
 * - 사용자 경험 개선을 위한 병목 지점 식별
 * - SLA 준수 모니터링
 * - 장애 조기 감지 및 대응
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;
    private final BusinessMetricsCollector businessMetricsCollector;
    
    // 요청별 타이머를 저장하는 맵
    private final ConcurrentHashMap<String, Timer.Sample> requestTimers = new ConcurrentHashMap<>();
    
    // 메트릭 태그 상수들
    private static final String ENDPOINT_TAG = "endpoint";
    private static final String METHOD_TAG = "method";
    private static final String STATUS_TAG = "status";
    private static final String USER_TYPE_TAG = "user_type";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = generateRequestId(request);
        Timer.Sample sample = Timer.start(meterRegistry);
        requestTimers.put(requestId, sample);
        
        // 요청 시작 메트릭 기록
        String endpoint = getEndpointName(request);
        String method = request.getMethod();
        
        meterRegistry.counter("http.requests.started.total",
                ENDPOINT_TAG, endpoint,
                METHOD_TAG, method)
                .increment();
        
        log.debug("HTTP 요청 시작 - {} {} (Request ID: {})", method, endpoint, requestId);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        String requestId = generateRequestId(request);
        Timer.Sample sample = requestTimers.remove(requestId);
        
        if (sample != null) {
            String endpoint = getEndpointName(request);
            String method = request.getMethod();
            String status = String.valueOf(response.getStatus());
            String userType = getUserType(request);
            
            // 응답 시간 기록
            sample.stop(Timer.builder("http.requests.duration")
                    .description("HTTP 요청 처리 시간")
                    .tag(ENDPOINT_TAG, endpoint)
                    .tag(METHOD_TAG, method)
                    .tag(STATUS_TAG, status)
                    .tag(USER_TYPE_TAG, userType)
                    .register(meterRegistry));
            
            // 요청 완료 카운터 증가
            meterRegistry.counter("http.requests.completed.total",
                    ENDPOINT_TAG, endpoint,
                    METHOD_TAG, method,
                    STATUS_TAG, status,
                    USER_TYPE_TAG, userType)
                    .increment();
            
            // 에러 상태 코드에 대한 별도 메트릭
            if (response.getStatus() >= 400) {
                meterRegistry.counter("http.requests.errors.total",
                        ENDPOINT_TAG, endpoint,
                        METHOD_TAG, method,
                        STATUS_TAG, status,
                        "error_type", getErrorType(response.getStatus()))
                        .increment();
                
                log.warn("HTTP 요청 에러 - {} {} 상태: {} (Request ID: {})", 
                        method, endpoint, status, requestId);
            }
            
            // 비즈니스 특화 메트릭 수집
            collectBusinessSpecificMetrics(request, response);
            
            log.debug("HTTP 요청 완료 - {} {} 상태: {} (Request ID: {})", 
                     method, endpoint, status, requestId);
        }
    }

    /**
     * 비즈니스 로직에 특화된 메트릭을 수집합니다.
     */
    private void collectBusinessSpecificMetrics(HttpServletRequest request, HttpServletResponse response) {
        String endpoint = getEndpointName(request);
        String method = request.getMethod();
        
        // 도서 관련 엔드포인트 메트릭
        if (endpoint.startsWith("/books")) {
            if (method.equals("POST") && response.getStatus() == 201) {
                // 도서 등록 성공
                businessMetricsCollector.recordBookRegistration();
            } else if (method.equals("GET") && endpoint.contains("/search")) {
                // 도서 검색 요청
                meterRegistry.counter("books.search.requests.total",
                        "search_type", getSearchType(request))
                        .increment();
            }
        }
        
        // 사용자 관련 엔드포인트 메트릭
        if (endpoint.startsWith("/auth")) {
            if (endpoint.contains("/authenticate") && response.getStatus() == 200) {
                // 로그인 성공
                String userId = extractUserIdFromRequest(request);
                if (userId != null) {
                    businessMetricsCollector.recordUserLogin(userId);
                }
            } else if (endpoint.contains("/register") && response.getStatus() == 202) {
                // 회원가입 성공
                businessMetricsCollector.recordUserRegistration();
            }
        }
        
        // 피드백 관련 엔드포인트 메트릭
        if (endpoint.startsWith("/feedbacks") && method.equals("POST") && response.getStatus() == 200) {
            // 피드백 제출 성공
            String bookId = extractBookIdFromRequest(request);
            Double rating = extractRatingFromRequest(request);
            businessMetricsCollector.recordFeedbackSubmission(bookId, rating);
        }
        
        // 대여/반납 관련 엔드포인트 메트릭
        if (endpoint.contains("/borrow") && method.equals("POST") && response.getStatus() == 200) {
            String bookId = extractBookIdFromRequest(request);
            String userId = extractUserIdFromRequest(request);
            businessMetricsCollector.recordBookBorrow(bookId, userId);
        } else if (endpoint.contains("/return") && method.equals("PATCH") && response.getStatus() == 200) {
            String bookId = extractBookIdFromRequest(request);
            String userId = extractUserIdFromRequest(request);
            businessMetricsCollector.recordBookReturn(bookId, userId);
        }
    }

    /**
     * 요청 고유 ID를 생성합니다.
     */
    private String generateRequestId(HttpServletRequest request) {
        return String.valueOf(request.hashCode()) + "_" + System.currentTimeMillis();
    }

    /**
     * 엔드포인트 이름을 정규화합니다.
     */
    private String getEndpointName(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        if (uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        
        // 숫자 ID를 {id}로 변환하여 메트릭 태그 개수 제한
        return uri.replaceAll("/\\d+", "/{id}")
                 .replaceAll("/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "/{uuid}");
    }

    /**
     * 사용자 타입을 결정합니다.
     */
    private String getUserType(HttpServletRequest request) {
        // JWT 토큰에서 사용자 역할 추출 (실제 구현은 JWT 파싱 로직 필요)
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "anonymous";
        }
        
        // 여기서는 간단히 처리, 실제로는 JWT 디코딩 필요
        // TODO: JWT 토큰에서 사용자 역할 추출 로직 구현
        return "authenticated";
    }

    /**
     * 에러 타입을 분류합니다.
     */
    private String getErrorType(int statusCode) {
        if (statusCode >= 400 && statusCode < 500) {
            return "client_error";
        } else if (statusCode >= 500) {
            return "server_error";
        }
        return "unknown";
    }

    /**
     * 검색 타입을 추출합니다.
     */
    private String getSearchType(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null) return "simple";
        
        if (query.contains("author")) return "by_author";
        if (query.contains("category")) return "by_category";
        if (query.contains("title")) return "by_title";
        
        return "complex";
    }

    /**
     * 요청에서 사용자 ID를 추출합니다.
     */
    private String extractUserIdFromRequest(HttpServletRequest request) {
        // 실제 구현에서는 JWT 토큰이나 세션에서 사용자 ID 추출
        // 여기서는 간단히 헤더에서 추출하는 것으로 가정
        return request.getHeader("X-User-Id");
    }

    /**
     * 요청에서 도서 ID를 추출합니다.
     */
    private String extractBookIdFromRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String[] segments = uri.split("/");
        
        // URI 패턴에서 도서 ID 추출 (/books/{bookId}/... 형태)
        for (int i = 0; i < segments.length; i++) {
            if ("books".equals(segments[i]) && i + 1 < segments.length) {
                return segments[i + 1];
            }
        }
        
        return null;
    }

    /**
     * 요청에서 평점을 추출합니다.
     */
    private Double extractRatingFromRequest(HttpServletRequest request) {
        // 실제 구현에서는 요청 본문이나 파라미터에서 평점 추출
        String ratingParam = request.getParameter("rating");
        if (ratingParam != null) {
            try {
                return Double.parseDouble(ratingParam);
            } catch (NumberFormatException e) {
                log.warn("평점 파싱 실패: {}", ratingParam);
            }
        }
        return null;
    }
}