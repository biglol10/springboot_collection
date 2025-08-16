package com.alibou.booknetwork.analytics;

import com.alibou.booknetwork.service.cache.RedisCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 사용자 행동 분석 추적기
 * 
 * 사용자 행동 분석의 엔터프라이즈 가치:
 * 1. 개인화 추천: 사용자 취향 기반 맞춤 콘텐츠 제공
 * 2. 사용자 경험 최적화: 행동 패턴 분석을 통한 UX 개선
 * 3. 비즈니스 인사이트: 데이터 기반 의사결정 지원
 * 4. 수익 최적화: 사용자 행동 기반 비즈니스 전략 수립
 * 
 * 데이터 수집 원칙:
 * - 개인정보 보호: GDPR, 개인정보보호법 준수
 * - 최소 수집: 목적에 필요한 최소한의 데이터만 수집
 * - 투명성: 사용자에게 데이터 수집 목적 명시
 * - 동의: 사용자 동의 기반 데이터 수집
 * - 보안: 수집된 데이터의 안전한 저장 및 처리
 * 
 * SSGD 행동 분석 패턴:
 * - 실시간 수집: 사용자 행동 즉시 캡처
 * - 배치 처리: 대용량 데이터의 효율적 처리
 * - 세션 기반 분석: 사용자 세션별 행동 패턴 추적
 * - 다차원 분석: 시간, 지역, 디바이스별 분석
 * - 예측 모델링: 머신러닝 기반 사용자 행동 예측
 * 
 * 수집 데이터 카테고리:
 * - 페이지 뷰: 방문 페이지, 체류 시간, 이탈률
 * - 상호작용: 클릭, 스크롤, 검색, 필터링
 * - 거래: 도서 대여, 반납, 평점, 리뷰
 * - 소셜: 공유, 좋아요, 팔로우, 댓글
 * - 기술적: 성능, 오류, 디바이스 정보
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorTracker {

    private final UserBehaviorRepository behaviorRepository;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    // 분석 카테고리 상수
    private static final String PAGE_VIEW_EVENT = "page_view";
    private static final String CLICK_EVENT = "click";
    private static final String SEARCH_EVENT = "search";
    private static final String BOOK_VIEW_EVENT = "book_view";
    private static final String BOOK_BORROW_EVENT = "book_borrow";
    private static final String BOOK_RETURN_EVENT = "book_return";
    private static final String RATING_EVENT = "rating";
    private static final String REVIEW_EVENT = "review";
    private static final String SOCIAL_EVENT = "social";
    private static final String ERROR_EVENT = "error";

    /**
     * 페이지 뷰 추적
     * 
     * 페이지 뷰 분석의 중요성:
     * - 사용자 관심 페이지 파악
     * - 페이지별 체류 시간 분석
     * - 사용자 여정(User Journey) 추적
     * - 이탈 페이지 식별
     * 
     * 수집 데이터:
     * - URL, 참조 페이지, 체류 시간
     * - 브라우저, OS, 디바이스 정보
     * - 지리적 위치 (IP 기반)
     * - 세션 정보
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackPageView(Integer userId, String page, String referrer, 
                                               HttpServletRequest request) {
        return CompletableFuture.runAsync(() -> {
            try {
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(PAGE_VIEW_EVENT)
                    .eventData(Map.of(
                        "page", page,
                        "referrer", referrer != null ? referrer : "direct",
                        "userAgent", request.getHeader("User-Agent"),
                        "clientIP", getClientIP(request),
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);
                
                // 실시간 분석을 위한 Redis 캐싱
                redisCacheService.trackUserActivity(userId, PAGE_VIEW_EVENT, 
                    Map.of("page", page, "timestamp", LocalDateTime.now().toString()));

                log.debug("페이지 뷰 추적 완료 - 사용자: {}, 페이지: {}", userId, page);

            } catch (Exception e) {
                log.error("페이지 뷰 추적 실패 - 사용자: {}, 페이지: {}, 오류: {}", 
                    userId, page, e.getMessage(), e);
            }
        });
    }

    /**
     * 클릭 이벤트 추적
     * 
     * 클릭 분석 활용:
     * - UI 요소별 인기도 측정
     * - 사용자 인터페이스 최적화
     * - A/B 테스트 성과 측정
     * - 버튼, 링크 효과성 분석
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackClick(Integer userId, String elementId, String elementType, 
                                            String page, Map<String, Object> context) {
        return CompletableFuture.runAsync(() -> {
            try {
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(CLICK_EVENT)
                    .eventData(Map.of(
                        "elementId", elementId,
                        "elementType", elementType,
                        "page", page,
                        "context", context != null ? context : Map.of(),
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);

                log.debug("클릭 이벤트 추적 완료 - 사용자: {}, 요소: {} ({})", 
                    userId, elementId, elementType);

            } catch (Exception e) {
                log.error("클릭 이벤트 추적 실패 - 사용자: {}, 요소: {}, 오류: {}", 
                    userId, elementId, e.getMessage(), e);
            }
        });
    }

    /**
     * 검색 행동 추적
     * 
     * 검색 분석 인사이트:
     * - 인기 검색어 및 트렌드
     * - 검색 결과 만족도
     * - 검색 개선 포인트 식별
     * - 콘텐츠 수요 예측
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackSearch(Integer userId, String query, String category, 
                                             int resultCount, boolean hasResults) {
        return CompletableFuture.runAsync(() -> {
            try {
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(SEARCH_EVENT)
                    .eventData(Map.of(
                        "query", query,
                        "category", category != null ? category : "all",
                        "resultCount", resultCount,
                        "hasResults", hasResults,
                        "queryLength", query.length(),
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);

                // 인기 검색어 통계 업데이트 (Redis)
                updatePopularSearchTerms(query);

                log.debug("검색 행동 추적 완료 - 사용자: {}, 검색어: '{}', 결과: {}개", 
                    userId, query, resultCount);

            } catch (Exception e) {
                log.error("검색 행동 추적 실패 - 사용자: {}, 검색어: '{}', 오류: {}", 
                    userId, query, e.getMessage(), e);
            }
        });
    }

    /**
     * 도서 조회 행동 추적
     * 
     * 도서 관심도 분석:
     * - 인기 도서 및 장르 식별
     * - 사용자별 선호 패턴 분석
     * - 추천 시스템 입력 데이터
     * - 재고 관리 인사이트
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackBookView(Integer userId, Integer bookId, String bookTitle, 
                                               String genre, String source, long viewDuration) {
        return CompletableFuture.runAsync(() -> {
            try {
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(BOOK_VIEW_EVENT)
                    .eventData(Map.of(
                        "bookId", bookId,
                        "bookTitle", bookTitle,
                        "genre", genre != null ? genre : "unknown",
                        "source", source, // 검색, 추천, 카테고리 등
                        "viewDuration", viewDuration, // 초 단위
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);

                // 도서 인기도 업데이트 (Redis)
                double popularityScore = calculatePopularityScore(viewDuration);
                redisCacheService.incrementBookPopularity(bookId, popularityScore);

                log.debug("도서 조회 추적 완료 - 사용자: {}, 도서: {} ({}), 조회시간: {}초", 
                    userId, bookTitle, bookId, viewDuration);

            } catch (Exception e) {
                log.error("도서 조회 추적 실패 - 사용자: {}, 도서: {}, 오류: {}", 
                    userId, bookId, e.getMessage(), e);
            }
        });
    }

    /**
     * 도서 대여 행동 추적
     * 
     * 대여 패턴 분석:
     * - 사용자별 대여 선호도
     * - 시간대별 대여 패턴
     * - 장르별 수요 분석
     * - 대여 전환율 분석
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackBookBorrow(Integer userId, Integer bookId, String bookTitle, 
                                                 String genre, String source, LocalDateTime dueDate) {
        return CompletableFuture.runAsync(() -> {
            try {
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(BOOK_BORROW_EVENT)
                    .eventData(Map.of(
                        "bookId", bookId,
                        "bookTitle", bookTitle,
                        "genre", genre != null ? genre : "unknown",
                        "source", source,
                        "dueDate", dueDate.toString(),
                        "borrowDayOfWeek", LocalDateTime.now().getDayOfWeek().toString(),
                        "borrowHour", LocalDateTime.now().getHour(),
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);

                // 사용자 선호 장르 업데이트
                updateUserGenrePreference(userId, genre);

                // 도서 대여 통계 업데이트
                redisCacheService.incrementBookPopularity(bookId, 5.0); // 대여는 높은 점수

                log.info("도서 대여 추적 완료 - 사용자: {}, 도서: {} ({})", userId, bookTitle, bookId);

            } catch (Exception e) {
                log.error("도서 대여 추적 실패 - 사용자: {}, 도서: {}, 오류: {}", 
                    userId, bookId, e.getMessage(), e);
            }
        });
    }

    /**
     * 도서 반납 행동 추적
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackBookReturn(Integer userId, Integer bookId, String bookTitle, 
                                                 LocalDateTime borrowDate, boolean isLate) {
        return CompletableFuture.runAsync(() -> {
            try {
                long borrowDays = java.time.Duration.between(borrowDate, LocalDateTime.now()).toDays();
                
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(BOOK_RETURN_EVENT)
                    .eventData(Map.of(
                        "bookId", bookId,
                        "bookTitle", bookTitle,
                        "borrowDate", borrowDate.toString(),
                        "borrowDays", borrowDays,
                        "isLate", isLate,
                        "returnDayOfWeek", LocalDateTime.now().getDayOfWeek().toString(),
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);

                // 사용자 대여 패턴 분석 데이터 업데이트
                updateUserBorrowPattern(userId, borrowDays, isLate);

                log.info("도서 반납 추적 완료 - 사용자: {}, 도서: {} ({}), 대여기간: {}일", 
                    userId, bookTitle, bookId, borrowDays);

            } catch (Exception e) {
                log.error("도서 반납 추적 실패 - 사용자: {}, 도서: {}, 오류: {}", 
                    userId, bookId, e.getMessage(), e);
            }
        });
    }

    /**
     * 평점 행동 추적
     * 
     * 평점 분석 활용:
     * - 도서 품질 평가
     * - 사용자 만족도 측정
     * - 추천 알고리즘 입력
     * - 콘텐츠 큐레이션
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackRating(Integer userId, Integer bookId, String bookTitle, 
                                             double rating, String previousRating) {
        return CompletableFuture.runAsync(() -> {
            try {
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(RATING_EVENT)
                    .eventData(Map.of(
                        "bookId", bookId,
                        "bookTitle", bookTitle,
                        "rating", rating,
                        "previousRating", previousRating != null ? previousRating : "none",
                        "isUpdate", previousRating != null,
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);

                // 사용자 평점 패턴 분석
                updateUserRatingPattern(userId, rating);

                log.debug("평점 행동 추적 완료 - 사용자: {}, 도서: {} ({}), 평점: {}", 
                    userId, bookTitle, bookId, rating);

            } catch (Exception e) {
                log.error("평점 행동 추적 실패 - 사용자: {}, 도서: {}, 오류: {}", 
                    userId, bookId, e.getMessage(), e);
            }
        });
    }

    /**
     * 리뷰 작성 행동 추적
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackReview(Integer userId, Integer bookId, String bookTitle, 
                                             int reviewLength, boolean hasImages) {
        return CompletableFuture.runAsync(() -> {
            try {
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(REVIEW_EVENT)
                    .eventData(Map.of(
                        "bookId", bookId,
                        "bookTitle", bookTitle,
                        "reviewLength", reviewLength,
                        "hasImages", hasImages,
                        "reviewCategory", categorizeReviewLength(reviewLength),
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);

                // 사용자 참여도 업데이트
                updateUserEngagement(userId, "review_written");

                log.debug("리뷰 작성 추적 완료 - 사용자: {}, 도서: {} ({}), 길이: {}자", 
                    userId, bookTitle, bookId, reviewLength);

            } catch (Exception e) {
                log.error("리뷰 작성 추적 실패 - 사용자: {}, 도서: {}, 오류: {}", 
                    userId, bookId, e.getMessage(), e);
            }
        });
    }

    /**
     * 소셜 행동 추적 (좋아요, 공유, 팔로우 등)
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackSocialAction(Integer userId, String actionType, 
                                                   String targetType, Integer targetId, Map<String, Object> context) {
        return CompletableFuture.runAsync(() -> {
            try {
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(SOCIAL_EVENT)
                    .eventData(Map.of(
                        "actionType", actionType, // like, share, follow, comment
                        "targetType", targetType, // book, user, review
                        "targetId", targetId,
                        "context", context != null ? context : Map.of(),
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);

                // 사용자 소셜 활동 점수 업데이트
                updateUserSocialScore(userId, actionType);

                log.debug("소셜 행동 추적 완료 - 사용자: {}, 액션: {}, 대상: {} ({})", 
                    userId, actionType, targetType, targetId);

            } catch (Exception e) {
                log.error("소셜 행동 추적 실패 - 사용자: {}, 액션: {}, 오류: {}", 
                    userId, actionType, e.getMessage(), e);
            }
        });
    }

    /**
     * 오류 이벤트 추적
     * 
     * 오류 분석 활용:
     * - 사용자 경험 문제점 식별
     * - 시스템 안정성 모니터링
     * - 개선 우선순위 결정
     * - 사용자 지원 개선
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackError(Integer userId, String errorType, String errorMessage, 
                                            String page, String userAgent) {
        return CompletableFuture.runAsync(() -> {
            try {
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(ERROR_EVENT)
                    .eventData(Map.of(
                        "errorType", errorType,
                        "errorMessage", errorMessage,
                        "page", page,
                        "userAgent", userAgent,
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);

                log.warn("오류 이벤트 추적 - 사용자: {}, 오류: {} ({}), 페이지: {}", 
                    userId, errorType, errorMessage, page);

            } catch (Exception e) {
                log.error("오류 이벤트 추적 실패 - 사용자: {}, 오류: {}", userId, e.getMessage(), e);
            }
        });
    }

    /**
     * 세션 종료 추적
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> trackSessionEnd(Integer userId, long sessionDuration, 
                                                  int pageViews, int interactions) {
        return CompletableFuture.runAsync(() -> {
            try {
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType("session_end")
                    .eventData(Map.of(
                        "sessionDuration", sessionDuration, // 초 단위
                        "pageViews", pageViews,
                        "interactions", interactions,
                        "engagementScore", calculateEngagementScore(sessionDuration, pageViews, interactions),
                        "timestamp", LocalDateTime.now()
                    ))
                    .build();

                saveEventAsync(event);

                // 사용자 세션 패턴 업데이트
                updateUserSessionPattern(userId, sessionDuration, pageViews, interactions);

                log.debug("세션 종료 추적 완료 - 사용자: {}, 세션시간: {}초, 페이지뷰: {}, 상호작용: {}", 
                    userId, sessionDuration, pageViews, interactions);

            } catch (Exception e) {
                log.error("세션 종료 추적 실패 - 사용자: {}, 오류: {}", userId, e.getMessage(), e);
            }
        });
    }

    /**
     * 헬퍼 메서드들
     */
    
    private void saveEventAsync(UserBehaviorEvent event) {
        try {
            behaviorRepository.save(event);
        } catch (Exception e) {
            log.error("행동 이벤트 저장 실패 - 사용자: {}, 이벤트: {}, 오류: {}", 
                event.getUserId(), event.getEventType(), e.getMessage(), e);
        }
    }

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

    private double calculatePopularityScore(long viewDuration) {
        // 조회 시간에 따른 인기도 점수 계산
        if (viewDuration < 10) return 0.5;      // 10초 미만: 낮은 관심
        if (viewDuration < 30) return 1.0;      // 30초 미만: 보통 관심  
        if (viewDuration < 60) return 2.0;      // 1분 미만: 높은 관심
        if (viewDuration < 300) return 3.0;     // 5분 미만: 매우 높은 관심
        return 5.0;                              // 5분 이상: 최고 관심
    }

    private String categorizeReviewLength(int length) {
        if (length < 50) return "short";
        if (length < 200) return "medium";
        if (length < 500) return "long";
        return "very_long";
    }

    private double calculateEngagementScore(long sessionDuration, int pageViews, int interactions) {
        // 세션 참여도 점수 계산 (0.0 ~ 10.0)
        double durationScore = Math.min(sessionDuration / 600.0, 5.0); // 최대 10분 기준
        double pageScore = Math.min(pageViews * 0.5, 2.5);            // 페이지뷰 기여
        double interactionScore = Math.min(interactions * 0.3, 2.5);   // 상호작용 기여
        
        return durationScore + pageScore + interactionScore;
    }

    private void updatePopularSearchTerms(String query) {
        try {
            // Redis에 인기 검색어 통계 업데이트 (스텁)
            redisCacheService.trackUserActivity(0, "search_term", 
                Map.of("query", query, "timestamp", LocalDateTime.now().toString()));
        } catch (Exception e) {
            log.warn("인기 검색어 업데이트 실패: {}", e.getMessage());
        }
    }

    private void updateUserGenrePreference(Integer userId, String genre) {
        try {
            // Redis에 사용자 장르 선호도 업데이트 (스텁)
            redisCacheService.trackUserActivity(userId, "genre_preference", 
                Map.of("genre", genre, "timestamp", LocalDateTime.now().toString()));
        } catch (Exception e) {
            log.warn("사용자 장르 선호도 업데이트 실패: {}", e.getMessage());
        }
    }

    private void updateUserBorrowPattern(Integer userId, long borrowDays, boolean isLate) {
        try {
            // 사용자 대여 패턴 분석 데이터 업데이트 (스텁)
            redisCacheService.trackUserActivity(userId, "borrow_pattern", 
                Map.of("borrowDays", borrowDays, "isLate", isLate, 
                       "timestamp", LocalDateTime.now().toString()));
        } catch (Exception e) {
            log.warn("사용자 대여 패턴 업데이트 실패: {}", e.getMessage());
        }
    }

    private void updateUserRatingPattern(Integer userId, double rating) {
        try {
            // 사용자 평점 패턴 분석 (스텁)
            redisCacheService.trackUserActivity(userId, "rating_pattern", 
                Map.of("rating", rating, "timestamp", LocalDateTime.now().toString()));
        } catch (Exception e) {
            log.warn("사용자 평점 패턴 업데이트 실패: {}", e.getMessage());
        }
    }

    private void updateUserEngagement(Integer userId, String engagementType) {
        try {
            // 사용자 참여도 업데이트 (스텁)
            redisCacheService.trackUserActivity(userId, "engagement", 
                Map.of("type", engagementType, "timestamp", LocalDateTime.now().toString()));
        } catch (Exception e) {
            log.warn("사용자 참여도 업데이트 실패: {}", e.getMessage());
        }
    }

    private void updateUserSocialScore(Integer userId, String actionType) {
        try {
            // 사용자 소셜 점수 업데이트 (스텁)
            double score = getSocialActionScore(actionType);
            redisCacheService.trackUserActivity(userId, "social_score", 
                Map.of("action", actionType, "score", score, "timestamp", LocalDateTime.now().toString()));
        } catch (Exception e) {
            log.warn("사용자 소셜 점수 업데이트 실패: {}", e.getMessage());
        }
    }

    private void updateUserSessionPattern(Integer userId, long sessionDuration, int pageViews, int interactions) {
        try {
            // 사용자 세션 패턴 업데이트 (스텁)
            redisCacheService.trackUserActivity(userId, "session_pattern", 
                Map.of("duration", sessionDuration, "pageViews", pageViews, 
                       "interactions", interactions, "timestamp", LocalDateTime.now().toString()));
        } catch (Exception e) {
            log.warn("사용자 세션 패턴 업데이트 실패: {}", e.getMessage());
        }
    }

    private double getSocialActionScore(String actionType) {
        switch (actionType.toLowerCase()) {
            case "like": return 1.0;
            case "share": return 2.0;
            case "comment": return 3.0;
            case "follow": return 5.0;
            default: return 0.5;
        }
    }
}