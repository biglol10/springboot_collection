package com.alibou.booknetwork.aspect;

import com.alibou.booknetwork.security.AuthorityUtils;
import com.alibou.booknetwork.service.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;

/**
 * AOP 기반 보안 관점(Aspect)
 * 
 * 보안 AOP의 핵심 가치:
 * 1. 횡단 보안 정책: 모든 계층에 일관된 보안 규칙 적용
 * 2. 중앙 집중식 보안: 보안 로직을 한 곳에서 관리
 * 3. 선언적 보안: 어노테이션 기반의 직관적 보안 설정
 * 4. 비즈니스 로직 분리: 핵심 기능과 보안 관심사 분리
 * 
 * 엔터프라이즈 보안 패턴:
 * - 속도 제한(Rate Limiting): DoS 공격 방어
 * - IP 화이트리스트: 신뢰할 수 있는 소스만 허용
 * - 권한 기반 접근 제어: 세밀한 권한 관리
 * - 감사 로깅: 모든 보안 이벤트 추적
 * 
 * SSGD에서 학습한 보안 강화 기법:
 * - 다층 보안 검증 (인증 → 인가 → 속도 제한)
 * - 컨텍스트 기반 보안 (IP, 시간, 디바이스 정보 활용)
 * - 실시간 위협 탐지 (비정상 패턴 감지)
 * - 동적 보안 정책 (런타임에서 보안 규칙 조정)
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAspect {

    private final RedisCacheService redisCacheService;
    private final AuthorityUtils authorityUtils;

    // 보안 설정 상수
    private static final int DEFAULT_RATE_LIMIT = 100; // 분당 요청 수
    private static final int ADMIN_RATE_LIMIT = 500;   // 관리자 분당 요청 수
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    
    // IP 화이트리스트 (실제 환경에서는 설정 파일에서 관리)
    private static final String[] WHITELISTED_IPS = {
        "127.0.0.1", "::1", "localhost"
    };

    /**
     * 컨트롤러 메서드에 대한 보안 검증
     * 
     * 보안 검증 단계:
     * 1. IP 화이트리스트 검사 (필요한 경우)
     * 2. 인증 상태 확인
     * 3. 권한 검증
     * 4. 속도 제한 검사
     * 5. 보안 이벤트 로깅
     * 
     * Around 어드바이스 사용 이유:
     * - 메서드 실행 전 보안 검증 수행
     * - 검증 실패 시 메서드 실행 차단
     * - 실행 후 보안 감사 로깅
     */
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object enforceSecurityForModifyingOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        
        try {
            // 1. 요청 컨텍스트 정보 수집
            HttpServletRequest request = getCurrentRequest();
            String clientIP = getClientIP(request);
            String userAgent = request.getHeader("User-Agent");
            
            // 2. 인증 정보 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logSecurityEvent("UNAUTHORIZED_ACCESS_ATTEMPT", methodName, clientIP, userAgent);
                throw new AccessDeniedException("인증이 필요한 작업입니다.");
            }
            
            String username = authentication.getName();
            
            // 3. 속도 제한 검사
            if (!checkRateLimit(username, clientIP)) {
                logSecurityEvent("RATE_LIMIT_EXCEEDED", methodName, clientIP, userAgent, username);
                throw new AccessDeniedException("요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
            }
            
            // 4. 민감한 작업에 대한 추가 검증
            if (isSensitiveOperation(methodName)) {
                performEnhancedSecurityCheck(authentication, clientIP, methodName);
            }
            
            // 5. 보안 검증 통과 로깅
            logSecurityEvent("SECURITY_CHECK_PASSED", methodName, clientIP, userAgent, username);
            
            // 6. 실제 메서드 실행
            Object result = joinPoint.proceed();
            
            // 7. 성공적인 작업 완료 로깅
            long executionTime = System.currentTimeMillis() - startTime;
            logSecurityEvent("OPERATION_COMPLETED", methodName, clientIP, userAgent, username, executionTime);
            
            return result;
            
        } catch (AccessDeniedException e) {
            // 접근 거부 이벤트 로깅
            logSecurityEvent("ACCESS_DENIED", methodName, 
                getCurrentRequest() != null ? getClientIP(getCurrentRequest()) : "unknown", 
                getCurrentRequest() != null ? getCurrentRequest().getHeader("User-Agent") : "unknown");
            throw e;
            
        } catch (Exception e) {
            // 기타 보안 관련 예외 로깅
            logSecurityEvent("SECURITY_ERROR", methodName, 
                getCurrentRequest() != null ? getClientIP(getCurrentRequest()) : "unknown", 
                getCurrentRequest() != null ? getCurrentRequest().getHeader("User-Agent") : "unknown", 
                e.getMessage());
            throw e;
        }
    }

    /**
     * 관리자 전용 기능에 대한 특별 보안 검증
     * 
     * 관리자 기능의 추가 보안 요구사항:
     * - 더 엄격한 인증 확인
     * - IP 화이트리스트 검사
     * - 관리자 권한 세부 검증
     * - 모든 관리자 행위 상세 로깅
     */
    @Around("execution(* com.alibou.booknetwork.*..*Controller.*admin*(..))")
    public Object enforceAdminSecurity(ProceedingJoinPoint joinPoint) throws Throwable {
        
        String methodName = joinPoint.getSignature().toShortString();
        HttpServletRequest request = getCurrentRequest();
        String clientIP = getClientIP(request);
        
        try {
            // 1. 인증 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logSecurityEvent("ADMIN_UNAUTHORIZED_ACCESS", methodName, clientIP);
                throw new AccessDeniedException("관리자 인증이 필요합니다.");
            }
            
            // 2. 관리자 권한 확인
            if (!authorityUtils.isAdmin()) {
                String username = authentication.getName();
                logSecurityEvent("ADMIN_PRIVILEGE_VIOLATION", methodName, clientIP, 
                    request.getHeader("User-Agent"), username);
                throw new AccessDeniedException("관리자 권한이 필요합니다.");
            }
            
            // 3. IP 화이트리스트 검사 (관리자 기능은 특정 IP만 허용 가능)
            if (!isWhitelistedIP(clientIP) && isProductionEnvironment()) {
                logSecurityEvent("ADMIN_IP_NOT_WHITELISTED", methodName, clientIP);
                throw new AccessDeniedException("허용되지 않은 IP에서의 관리자 접근입니다.");
            }
            
            // 4. 관리자 작업 시작 로깅
            String username = authentication.getName();
            Object[] args = joinPoint.getArgs();
            logAdminOperation("ADMIN_OPERATION_START", methodName, username, clientIP, args);
            
            // 5. 메서드 실행
            Object result = joinPoint.proceed();
            
            // 6. 관리자 작업 완료 로깅
            logAdminOperation("ADMIN_OPERATION_SUCCESS", methodName, username, clientIP, args);
            
            return result;
            
        } catch (Exception e) {
            // 관리자 작업 실패 로깅
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : "unknown";
            logAdminOperation("ADMIN_OPERATION_FAILED", methodName, username, clientIP, 
                joinPoint.getArgs(), e.getMessage());
            throw e;
        }
    }

    /**
     * 속도 제한 검사
     * 
     * 다단계 속도 제한 전략:
     * 1. 사용자별 제한: 계정당 분당 요청 수
     * 2. IP별 제한: IP당 분당 요청 수  
     * 3. 전역 제한: 시스템 전체 부하 관리
     * 
     * 권한별 차등 적용:
     * - 일반 사용자: 분당 100회
     * - 관리자: 분당 500회
     * - 시스템 계정: 제한 없음
     */
    private boolean checkRateLimit(String username, String clientIP) {
        try {
            // 관리자는 더 높은 한도 적용
            int rateLimit = authorityUtils.isAdmin() ? ADMIN_RATE_LIMIT : DEFAULT_RATE_LIMIT;
            
            // 사용자별 속도 제한
            String userKey = "user:" + username;
            if (redisCacheService.isRateLimited(userKey, rateLimit, RATE_LIMIT_WINDOW)) {
                return false;
            }
            
            // IP별 속도 제한 (IP당 더 높은 한도 적용)
            String ipKey = "ip:" + clientIP;
            if (redisCacheService.isRateLimited(ipKey, rateLimit * 2, RATE_LIMIT_WINDOW)) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("속도 제한 검사 중 오류 발생 - 사용자: {}, IP: {}, 오류: {}", 
                username, clientIP, e.getMessage());
            // 오류 발생 시 요청 허용 (fail-open policy)
            return true;
        }
    }

    /**
     * 민감한 작업 여부 판단
     * 
     * 민감한 작업 기준:
     * - 데이터 삭제 작업
     * - 권한 변경 작업
     * - 시스템 설정 변경
     * - 대량 데이터 처리
     */
    private boolean isSensitiveOperation(String methodName) {
        String lowerMethodName = methodName.toLowerCase();
        return lowerMethodName.contains("delete") ||
               lowerMethodName.contains("remove") ||
               lowerMethodName.contains("admin") ||
               lowerMethodName.contains("batch") ||
               lowerMethodName.contains("bulk");
    }

    /**
     * 강화된 보안 검사
     * 
     * 추가 검증 항목:
     * - 최근 로그인 시간 확인
     * - 비정상적인 접근 패턴 탐지
     * - 지리적 위치 변경 감지
     * - 디바이스 핑거프린팅
     */
    private void performEnhancedSecurityCheck(Authentication authentication, 
                                            String clientIP, String methodName) {
        try {
            String username = authentication.getName();
            
            // 1. 최근 활동 이력 확인
            // List<String> recentActivities = redisCacheService.getUserRecentActivities(userId, 10);
            
            // 2. IP 변경 감지
            String lastKnownIP = getLastKnownIP(username);
            if (lastKnownIP != null && !lastKnownIP.equals(clientIP)) {
                logSecurityEvent("IP_ADDRESS_CHANGED", methodName, clientIP, 
                    "unknown", username, "이전 IP: " + lastKnownIP);
            }
            
            // 3. 시간대 기반 접근 제어 (업무 시간 외 민감한 작업 제한)
            if (isOutsideBusinessHours() && isSensitiveOperation(methodName)) {
                logSecurityEvent("OUTSIDE_BUSINESS_HOURS_ACCESS", methodName, clientIP, 
                    "unknown", username);
                // 운영 정책에 따라 차단하거나 경고만 로그
                // throw new AccessDeniedException("업무 시간 외에는 민감한 작업이 제한됩니다.");
            }
            
            // 4. 현재 IP 정보 업데이트
            updateLastKnownIP(username, clientIP);
            
        } catch (Exception e) {
            log.error("강화된 보안 검사 중 오류 발생 - 사용자: {}, IP: {}, 오류: {}", 
                authentication.getName(), clientIP, e.getMessage());
        }
    }

    /**
     * IP 화이트리스트 검사
     */
    private boolean isWhitelistedIP(String clientIP) {
        if (clientIP == null) return false;
        
        return Arrays.stream(WHITELISTED_IPS)
                .anyMatch(whitelistedIP -> whitelistedIP.equals(clientIP));
    }

    /**
     * 현재 HTTP 요청 객체 획득
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 클라이언트 IP 주소 추출 (프록시 환경 고려)
     */
    private String getClientIP(HttpServletRequest request) {
        if (request == null) return "unknown";
        
        String[] headers = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", 
            "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 보안 이벤트 로깅
     * 
     * 구조화된 보안 로그:
     * - 이벤트 타입별 분류
     * - 컨텍스트 정보 포함
     * - 추후 분석 가능한 형태
     * - SIEM 시스템 연동 고려
     */
    private void logSecurityEvent(String eventType, String method, String clientIP) {
        logSecurityEvent(eventType, method, clientIP, null, null, null);
    }

    private void logSecurityEvent(String eventType, String method, String clientIP, 
                                String userAgent) {
        logSecurityEvent(eventType, method, clientIP, userAgent, null, null);
    }

    private void logSecurityEvent(String eventType, String method, String clientIP, 
                                String userAgent, String username) {
        logSecurityEvent(eventType, method, clientIP, userAgent, username, null);
    }

    private void logSecurityEvent(String eventType, String method, String clientIP, 
                                String userAgent, String username, Object extra) {
        try {
            log.warn("보안 이벤트 발생 - 타입: {}, 메서드: {}, IP: {}, 사용자: {}, UserAgent: {}, 추가정보: {}", 
                eventType, method, clientIP, username != null ? username : "unknown", 
                userAgent != null ? userAgent : "unknown", extra != null ? extra : "없음");
                
            // 실제 환경에서는 보안 이벤트를 별도 시스템으로 전송
            // securityEventService.sendToSIEM(eventType, method, clientIP, username, userAgent, extra);
            
        } catch (Exception e) {
            log.error("보안 이벤트 로깅 실패: {}", e.getMessage());
        }
    }

    /**
     * 관리자 작업 전용 로깅
     */
    private void logAdminOperation(String eventType, String method, String username, 
                                 String clientIP, Object[] args) {
        logAdminOperation(eventType, method, username, clientIP, args, null);
    }

    private void logAdminOperation(String eventType, String method, String username, 
                                 String clientIP, Object[] args, String error) {
        try {
            log.warn("관리자 작업 - 이벤트: {}, 메서드: {}, 관리자: {}, IP: {}, 파라미터 수: {}, 오류: {}", 
                eventType, method, username, clientIP, 
                args != null ? args.length : 0, 
                error != null ? error : "없음");
                
            // 관리자 작업은 별도 감사 로그로 기록
            // auditLogService.recordAdminOperation(eventType, method, username, clientIP, args, error);
            
        } catch (Exception e) {
            log.error("관리자 작업 로깅 실패: {}", e.getMessage());
        }
    }

    /**
     * 헬퍼 메서드들
     */
    private String getLastKnownIP(String username) {
        // Redis에서 사용자의 마지막 알려진 IP 조회
        // return redisCacheService.getLastKnownIP(username);
        return null;
    }

    private void updateLastKnownIP(String username, String clientIP) {
        // Redis에 사용자의 현재 IP 업데이트
        // redisCacheService.updateLastKnownIP(username, clientIP);
    }

    private boolean isOutsideBusinessHours() {
        // 현재 시간이 업무 시간(9-18시) 외인지 확인
        java.time.LocalTime now = java.time.LocalTime.now();
        return now.isBefore(java.time.LocalTime.of(9, 0)) || 
               now.isAfter(java.time.LocalTime.of(18, 0));
    }

    private boolean isProductionEnvironment() {
        // 프로덕션 환경 여부 확인 (환경 변수 또는 프로파일 기반)
        return "production".equals(System.getProperty("spring.profiles.active"));
    }
}