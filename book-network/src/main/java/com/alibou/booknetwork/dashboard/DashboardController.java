package com.alibou.booknetwork.dashboard;

import com.alibou.booknetwork.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 실시간 대시보드 REST API 컨트롤러
 * 
 * 대시보드 API의 설계 원칙:
 * 1. RESTful 설계: 자원 중심의 명확한 URL 구조
 * 2. 보안 적용: JWT 기반 인증 및 권한 검증
 * 3. 성능 최적화: 캐싱, 비동기 처리, 압축
 * 4. 실시간 지원: SSE를 통한 실시간 데이터 스트리밍
 * 5. 문서화: Swagger/OpenAPI 기반 API 문서
 * 
 * API 엔드포인트 구조:
 * - GET /api/dashboard/admin - 관리자 대시보드
 * - GET /api/dashboard/user/{userId} - 사용자 개인 대시보드
 * - GET /api/dashboard/book/{bookId} - 도서 분석 대시보드
 * - GET /api/dashboard/realtime/stream - 실시간 데이터 스트림
 * - POST /api/dashboard/export - 데이터 내보내기
 * 
 * 보안 고려사항:
 * - 관리자 대시보드: ADMIN 권한 필요
 * - 사용자 대시보드: 본인 또는 ADMIN 권한
 * - 실시간 스트림: 유효한 JWT 토큰 필요
 * - Rate Limiting: API 호출 빈도 제한
 * 
 * 에러 처리:
 * - 400: 잘못된 요청 파라미터
 * - 401: 인증 실패
 * - 403: 권한 부족
 * - 404: 리소스 없음
 * - 500: 서버 내부 오류
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "대시보드 API", description = "실시간 대시보드 및 분석 데이터 제공")
public class DashboardController {

    private final DashboardService dashboardService;
    private final JwtService jwtService;

    /**
     * 관리자 대시보드 조회
     * 
     * 시스템 전체 현황을 종합적으로 제공하는 관리자 전용 대시보드
     * 
     * 제공 데이터:
     * - 전체 통계: 사용자, 도서, 대여 현황
     * - 실시간 활동: 현재 접속자, 오늘의 활동
     * - KPI 지표: 참여율, 이용률, 만족도 등
     * - 트렌드 분석: 30일간 사용 패턴
     * - 인기 콘텐츠: 도서, 장르, 저자
     * - 시스템 상태: 서버, DB, 캐시 상태
     * 
     * @return 관리자 대시보드 데이터
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "관리자 대시보드 조회", 
        description = "시스템 전체 현황 및 통계 데이터를 제공합니다. 관리자 권한이 필요합니다."
    )
    public ResponseEntity<DashboardData> getAdminDashboard(HttpServletRequest request) {
        try {
            log.info("관리자 대시보드 요청 - IP: {}", getClientIP(request));
            
            DashboardData dashboardData = dashboardService.getAdminDashboard();
            
            if (dashboardData.isValid()) {
                log.info("관리자 대시보드 조회 성공");
                return ResponseEntity.ok(dashboardData);
            } else {
                log.warn("관리자 대시보드 데이터 불완전");
                return ResponseEntity.internalServerError().body(dashboardData);
            }
            
        } catch (Exception e) {
            log.error("관리자 대시보드 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(DashboardData.builder()
                    .dashboardType("admin")
                    .error("관리자 대시보드 조회 중 오류가 발생했습니다.")
                    .build());
        }
    }

    /**
     * 사용자 개인 대시보드 조회
     * 
     * 개별 사용자의 독서 활동 및 개인화된 정보 제공
     * 
     * 제공 데이터:
     * - 개인 통계: 읽은 도서 수, 작성한 리뷰, 평균 평점
     * - 읽기 활동: 현재 읽는 책, 최근 완독 목록
     * - 소셜 활동: 팔로워, 좋아요, 댓글 현황
     * - 맞춤 추천: AI 기반 개인 추천 목록
     * - 목표 진행률: 연간 독서 목표 달성 현황
     * - 활동 타임라인: 최근 활동 이벤트
     * 
     * @param userId 조회할 사용자 ID
     * @return 사용자 개인 대시보드 데이터
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    @Operation(
        summary = "사용자 개인 대시보드 조회", 
        description = "개별 사용자의 독서 활동 및 개인화된 정보를 제공합니다. 본인 또는 관리자만 조회 가능합니다."
    )
    public ResponseEntity<DashboardData> getUserDashboard(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Integer userId,
            HttpServletRequest request) {
        try {
            log.info("사용자 대시보드 요청 - 사용자: {}, IP: {}", userId, getClientIP(request));
            
            DashboardData dashboardData = dashboardService.getUserDashboard(userId);
            
            if (dashboardData.isValid()) {
                log.info("사용자 대시보드 조회 성공 - 사용자: {}", userId);
                return ResponseEntity.ok(dashboardData);
            } else {
                log.warn("사용자 대시보드 데이터 불완전 - 사용자: {}", userId);
                return ResponseEntity.internalServerError().body(dashboardData);
            }
            
        } catch (Exception e) {
            log.error("사용자 대시보드 조회 실패 - 사용자: {}, 오류: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(DashboardData.builder()
                    .dashboardType("user")
                    .userId(userId)
                    .error("사용자 대시보드 조회 중 오류가 발생했습니다.")
                    .build());
        }
    }

    /**
     * 도서 분석 대시보드 조회
     * 
     * 특정 도서의 상세 분석 정보 제공
     * 
     * 제공 데이터:
     * - 도서 통계: 대여 횟수, 리뷰 수, 평균 평점
     * - 사용자 참여도: 참여 점수, 고유 대여자 수
     * - 평점 분석: 평점 분포, 감정 분석, 키워드
     * - 대여 패턴: 시간대별, 요일별, 계절별 패턴
     * 
     * @param bookId 조회할 도서 ID
     * @return 도서 분석 대시보드 데이터
     */
    @GetMapping("/book/{bookId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
    @Operation(
        summary = "도서 분석 대시보드 조회", 
        description = "특정 도서의 상세 분석 정보를 제공합니다. 관리자 또는 사서 권한이 필요합니다."
    )
    public ResponseEntity<DashboardData> getBookAnalyticsDashboard(
            @Parameter(description = "도서 ID", required = true)
            @PathVariable Integer bookId,
            HttpServletRequest request) {
        try {
            log.info("도서 분석 대시보드 요청 - 도서: {}, IP: {}", bookId, getClientIP(request));
            
            DashboardData dashboardData = dashboardService.getBookAnalyticsDashboard(bookId);
            
            if (dashboardData.isValid()) {
                log.info("도서 분석 대시보드 조회 성공 - 도서: {}", bookId);
                return ResponseEntity.ok(dashboardData);
            } else {
                log.warn("도서 분석 대시보드 데이터 불완전 - 도서: {}", bookId);
                return ResponseEntity.internalServerError().body(dashboardData);
            }
            
        } catch (Exception e) {
            log.error("도서 분석 대시보드 조회 실패 - 도서: {}, 오류: {}", bookId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(DashboardData.builder()
                    .dashboardType("book")
                    .bookId(bookId)
                    .error("도서 분석 대시보드 조회 중 오류가 발생했습니다.")
                    .build());
        }
    }

    /**
     * 실시간 통계 스트림 (Server-Sent Events)
     * 
     * WebSocket 대신 SSE를 사용하여 실시간 대시보드 데이터 스트리밍
     * 
     * 스트리밍 데이터:
     * - 현재 접속자 수
     * - 실시간 대여/반납 현황
     * - 오늘의 가입자 수
     * - 시스템 부하 상태
     * - 인기 도서 변동
     * 
     * 연결 특성:
     * - 30분 타임아웃
     * - 30초마다 데이터 전송
     * - 자동 재연결 지원
     * - JWT 토큰 검증
     * 
     * @param token JWT 인증 토큰
     * @param lastEventId 마지막 이벤트 ID (재연결 시)
     * @return SSE 스트림
     */
    @GetMapping(value = "/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "실시간 통계 스트림", 
        description = "Server-Sent Events를 통해 실시간 대시보드 데이터를 스트리밍합니다."
    )
    public ResponseEntity<SseEmitter> streamRealtimeStats(
            @Parameter(description = "JWT 인증 토큰")
            @RequestParam(name = "token", required = false) String token,
            @Parameter(description = "마지막 이벤트 ID (재연결용)")
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            HttpServletRequest request) {
        
        try {
            // JWT 토큰 검증
            Integer userId = null;
            if (token != null && !token.trim().isEmpty()) {
                try {
                    String userEmail = jwtService.extractUsername(token);
                    if (jwtService.isTokenValid(token, userEmail)) {
                        // 실제로는 UserService를 통해 userId 조회
                        userId = 1; // 스텁
                        log.info("실시간 스트림 인증 성공 - 사용자: {}", userId);
                    } else {
                        log.warn("실시간 스트림 토큰 무효");
                        return ResponseEntity.status(401).build();
                    }
                } catch (Exception e) {
                    log.warn("실시간 스트림 토큰 검증 실패: {}", e.getMessage());
                    return ResponseEntity.status(401).build();
                }
            } else {
                log.warn("실시간 스트림 토큰 누락");
                return ResponseEntity.status(401).build();
            }

            log.info("실시간 통계 스트림 시작 - 사용자: {}, IP: {}", userId, getClientIP(request));

            // SSE 연결 생성 (30분 타임아웃)
            SseEmitter emitter = new SseEmitter(1800000L);
            
            // 연결 성공 이벤트 전송
            emitter.send(SseEmitter.event()
                .id("connect-" + System.currentTimeMillis())
                .name("connected")
                .data(Map.of(
                    "message", "실시간 스트림 연결됨",
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
                )));

            // 미수신 이벤트 재전송 (재연결 시)
            if (lastEventId != null && !lastEventId.trim().isEmpty()) {
                log.debug("재연결 감지 - 마지막 이벤트ID: {}", lastEventId);
                // 실제로는 Redis에서 미수신 이벤트 조회 후 재전송
            }

            // 주기적 데이터 전송 스케줄링 (비동기)
            Integer finalUserId = userId;
            CompletableFuture.runAsync(() -> {
                try {
                    while (!emitter.isTimedOut()) {
                        Thread.sleep(30000); // 30초마다 전송
                        
                        // 실시간 통계 조회
                        CompletableFuture<RealtimeStats> statsFuture = dashboardService.getRealtimeStats();
                        RealtimeStats stats = statsFuture.get();
                        
                        if (stats.isHealthy()) {
                            emitter.send(SseEmitter.event()
                                .id("stats-" + System.currentTimeMillis())
                                .name("realtime-stats")
                                .data(stats));
                        }
                    }
                } catch (Exception e) {
                    log.warn("실시간 스트림 데이터 전송 중 오류 - 사용자: {}, 오류: {}", 
                        finalUserId, e.getMessage());
                    emitter.completeWithError(e);
                }
            });

            // 연결 종료 시 정리
            emitter.onCompletion(() -> {
                log.info("실시간 스트림 정상 종료 - 사용자: {}", finalUserId);
            });

            emitter.onTimeout(() -> {
                log.info("실시간 스트림 타임아웃 - 사용자: {}", finalUserId);
            });

            emitter.onError((throwable) -> {
                log.warn("실시간 스트림 오류 - 사용자: {}, 오류: {}", finalUserId, throwable.getMessage());
            });

            return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(emitter);

        } catch (Exception e) {
            log.error("실시간 스트림 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 대시보드 데이터 내보내기
     * 
     * 대시보드 데이터를 Excel, PDF 등의 형식으로 내보내기
     * 
     * 지원 형식:
     * - Excel (.xlsx)
     * - PDF (.pdf)
     * - CSV (.csv)
     * - JSON (.json)
     * 
     * @param request 내보내기 요청 정보
     * @return 내보내기 작업 결과
     */
    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
    @Operation(
        summary = "대시보드 데이터 내보내기", 
        description = "대시보드 데이터를 지정된 형식으로 내보냅니다. 관리자 또는 사서 권한이 필요합니다."
    )
    public ResponseEntity<Map<String, Object>> exportDashboardData(
            @RequestBody ExportRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("대시보드 데이터 내보내기 요청 - 타입: {}, 형식: {}, IP: {}", 
                request.getDashboardType(), request.getFormat(), getClientIP(httpRequest));

            // 비동기로 내보내기 작업 시작
            CompletableFuture<String> exportFuture = dashboardService.exportDashboardData(
                request.getDashboardType(),
                request.getFormat(),
                request.getStartDate(),
                request.getEndDate()
            );

            // 작업 ID 생성 (실제로는 UUID 사용)
            String taskId = "export-" + System.currentTimeMillis();

            Map<String, Object> response = Map.of(
                "taskId", taskId,
                "status", "processing",
                "message", "데이터 내보내기가 시작되었습니다.",
                "estimatedTime", "2-5분"
            );

            log.info("대시보드 데이터 내보내기 시작 - 작업ID: {}", taskId);
            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("대시보드 데이터 내보내기 실패: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "message", "데이터 내보내기 중 오류가 발생했습니다.",
                "error", e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 내보내기 작업 상태 확인
     * 
     * @param taskId 작업 ID
     * @return 작업 상태
     */
    @GetMapping("/export/{taskId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
    @Operation(
        summary = "내보내기 작업 상태 확인", 
        description = "내보내기 작업의 진행 상태를 확인합니다."
    )
    public ResponseEntity<Map<String, Object>> getExportStatus(
            @Parameter(description = "작업 ID", required = true)
            @PathVariable String taskId) {
        try {
            // 실제로는 Redis나 DB에서 작업 상태 조회
            Map<String, Object> status = Map.of(
                "taskId", taskId,
                "status", "completed", // processing, completed, failed
                "progress", 100,
                "downloadUrl", "/api/dashboard/export/" + taskId + "/download",
                "completedAt", System.currentTimeMillis()
            );

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("내보내기 작업 상태 확인 실패 - 작업ID: {}, 오류: {}", taskId, e.getMessage(), e);
            Map<String, Object> errorResponse = Map.of(
                "taskId", taskId,
                "status", "error",
                "message", "작업 상태 확인 중 오류가 발생했습니다."
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 헬퍼 메서드들
     */
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.trim().isEmpty()) {
            return xRealIP.trim();
        }
        
        return request.getRemoteAddr();
    }
}

/**
 * 내보내기 요청 DTO
 */
@Data
class ExportRequest {
    private String dashboardType; // admin, user, book
    private String format; // excel, pdf, csv, json
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private Map<String, Object> filters; // 추가 필터 옵션
}