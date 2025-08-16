package com.alibou.booknetwork.recommendation;

import com.alibou.booknetwork.analytics.UserBehaviorRepository;
import com.alibou.booknetwork.book.Book;
import com.alibou.booknetwork.book.BookRepository;
import com.alibou.booknetwork.feedback.FeedbackRepository;
import com.alibou.booknetwork.service.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI 기반 도서 추천 엔진
 * 
 * 추천 시스템의 엔터프라이즈 가치:
 * 1. 개인화 경험: 사용자별 맞춤 콘텐츠 제공으로 만족도 향상
 * 2. 매출 증대: 관련성 높은 추천으로 대여율 30-50% 향상
 * 3. 사용자 참여: 체류 시간 증가 및 재방문율 향상
 * 4. 운영 효율성: 자동화된 큐레이션으로 인건비 절약
 * 5. 경쟁 우위: 차별화된 서비스로 시장 선도
 * 
 * 추천 알고리즘 종류:
 * 1. 협업 필터링 (Collaborative Filtering)
 *    - 사용자 기반: 유사한 취향의 사용자 추천
 *    - 아이템 기반: 유사한 도서 추천
 * 2. 콘텐츠 기반 (Content-Based)
 *    - 도서 메타데이터 기반 추천
 * 3. 하이브리드 (Hybrid)
 *    - 여러 방법론 조합으로 정확도 향상
 * 
 * SSGD 추천 시스템 패턴:
 * - 실시간 추천: 사용자 행동 즉시 반영
 * - 다중 신호: 평점, 대여, 조회, 검색 등 복합 고려
 * - 콜드 스타트 해결: 신규 사용자/도서 문제 대응
 * - A/B 테스팅: 추천 알고리즘 성능 지속 개선
 * - 설명 가능성: 추천 이유 제공으로 신뢰도 향상
 * 
 * 성능 최적화:
 * - 사전 계산: 유사도 매트릭스 배치 처리
 * - 캐싱: 추천 결과 Redis 저장
 * - 근사 알고리즘: 대용량 데이터 처리 최적화
 * - 분산 처리: 병렬 계산으로 응답 시간 단축
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationEngine {

    private final BookRepository bookRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserBehaviorRepository behaviorRepository;
    private final RedisCacheService redisCacheService;

    // 추천 캐시 TTL (Time To Live)
    private static final long RECOMMENDATION_CACHE_TTL = 3600; // 1시간
    private static final int DEFAULT_RECOMMENDATION_COUNT = 10;
    private static final double MIN_SIMILARITY_THRESHOLD = 0.1;

    /**
     * 사용자별 개인화 추천 (메인 추천 API)
     * 
     * 하이브리드 추천 전략:
     * 1. 협업 필터링 추천 (60% 가중치)
     * 2. 콘텐츠 기반 추천 (30% 가중치)  
     * 3. 인기도 기반 추천 (10% 가중치)
     * 
     * @param userId 대상 사용자 ID
     * @param count 추천할 도서 수
     * @return 추천 도서 목록 (점수 순 정렬)
     */
    public List<RecommendationResult> getPersonalizedRecommendations(Integer userId, int count) {
        try {
            log.info("개인화 추천 시작 - 사용자: {}, 요청 수: {}", userId, count);
            
            // 1. 캐시에서 기존 추천 확인
            List<RecommendationResult> cachedResults = getCachedRecommendations(userId);
            if (cachedResults != null && !cachedResults.isEmpty()) {
                log.debug("추천 캐시 히트 - 사용자: {}, 캐시된 추천 수: {}", userId, cachedResults.size());
                return cachedResults.stream().limit(count).collect(Collectors.toList());
            }

            // 2. 하이브리드 추천 생성
            List<RecommendationResult> recommendations = new ArrayList<>();
            
            // 협업 필터링 추천 (60%)
            int collaborativeCount = (int) (count * 0.6);
            List<RecommendationResult> collaborativeResults = 
                getCollaborativeFilteringRecommendations(userId, collaborativeCount);
            recommendations.addAll(collaborativeResults);

            // 콘텐츠 기반 추천 (30%)
            int contentCount = (int) (count * 0.3);
            List<RecommendationResult> contentResults = 
                getContentBasedRecommendations(userId, contentCount);
            recommendations.addAll(contentResults);

            // 인기도 기반 추천 (10%) - 다양성 확보
            int popularityCount = count - collaborativeCount - contentCount;
            List<RecommendationResult> popularityResults = 
                getPopularityBasedRecommendations(userId, popularityCount);
            recommendations.addAll(popularityResults);

            // 3. 중복 제거 및 점수 정규화
            List<RecommendationResult> finalResults = 
                deduplicateAndNormalize(recommendations, count);

            // 4. 추천 결과 캐싱
            cacheRecommendations(userId, finalResults);

            // 5. 추천 로깅 (분석용)
            logRecommendationEvent(userId, finalResults);

            log.info("개인화 추천 완료 - 사용자: {}, 생성된 추천 수: {}", userId, finalResults.size());
            return finalResults;

        } catch (Exception e) {
            log.error("개인화 추천 실패 - 사용자: {}, 오류: {}", userId, e.getMessage(), e);
            // 폴백: 인기 도서 추천
            return getPopularityBasedRecommendations(userId, count);
        }
    }

    /**
     * 협업 필터링 추천 (사용자 기반)
     * 
     * 알고리즘 원리:
     * 1. 대상 사용자와 유사한 취향의 사용자들 찾기
     * 2. 유사 사용자들이 좋아한 도서 수집
     * 3. 대상 사용자가 아직 읽지 않은 도서 필터링
     * 4. 유사도 가중 평균으로 추천 점수 계산
     * 
     * 유사도 계산 방법:
     * - 코사인 유사도: 평점 벡터 간 각도
     * - 피어슨 상관계수: 평점 패턴 유사성
     * - 자카드 유사도: 공통 대여 도서 비율
     */
    private List<RecommendationResult> getCollaborativeFilteringRecommendations(Integer userId, int count) {
        try {
            log.debug("협업 필터링 추천 시작 - 사용자: {}", userId);

            // 1. 사용자의 평점/대여 이력 조회
            Map<Integer, Double> userRatings = getUserRatings(userId);
            if (userRatings.isEmpty()) {
                log.debug("사용자 평점 이력 없음 - 협업 필터링 불가: {}", userId);
                return new ArrayList<>();
            }

            // 2. 유사한 사용자들 찾기
            List<UserSimilarity> similarUsers = findSimilarUsers(userId, userRatings);
            if (similarUsers.isEmpty()) {
                log.debug("유사 사용자 없음 - 협업 필터링 불가: {}", userId);
                return new ArrayList<>();
            }

            // 3. 유사 사용자들의 추천 도서 수집
            Map<Integer, Double> bookScores = new HashMap<>();
            for (UserSimilarity similar : similarUsers) {
                Map<Integer, Double> similarUserRatings = getUserRatings(similar.getUserId());
                
                for (Map.Entry<Integer, Double> entry : similarUserRatings.entrySet()) {
                    Integer bookId = entry.getKey();
                    Double rating = entry.getValue();
                    
                    // 대상 사용자가 이미 평가한 도서는 제외
                    if (!userRatings.containsKey(bookId)) {
                        // 유사도 가중 점수 계산
                        double weightedScore = rating * similar.getSimilarity();
                        bookScores.merge(bookId, weightedScore, Double::sum);
                    }
                }
            }

            // 4. 점수 순으로 정렬하여 추천 결과 생성
            return bookScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(count)
                .map(entry -> {
                    Book book = bookRepository.findById(entry.getKey()).orElse(null);
                    if (book != null) {
                        return RecommendationResult.builder()
                            .bookId(entry.getKey())
                            .title(book.getTitle())
                            .author(book.getAuthorName())
                            .score(entry.getValue())
                            .reason("비슷한 취향의 사용자들이 좋아한 도서")
                            .algorithm("collaborative_filtering")
                            .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("협업 필터링 추천 실패 - 사용자: {}, 오류: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 콘텐츠 기반 추천
     * 
     * 알고리즘 원리:
     * 1. 사용자가 선호한 도서들의 특성 분석
     * 2. 유사한 특성을 가진 다른 도서들 찾기
     * 3. 특성 유사도 기반 추천 점수 계산
     * 
     * 고려하는 도서 특성:
     * - 장르/카테고리
     * - 저자
     * - 출판연도
     * - 페이지 수
     * - 키워드/태그
     */
    private List<RecommendationResult> getContentBasedRecommendations(Integer userId, int count) {
        try {
            log.debug("콘텐츠 기반 추천 시작 - 사용자: {}", userId);

            // 1. 사용자 선호 도서 분석
            List<Book> userLikedBooks = getUserLikedBooks(userId);
            if (userLikedBooks.isEmpty()) {
                log.debug("사용자 선호 도서 없음 - 콘텐츠 기반 추천 불가: {}", userId);
                return new ArrayList<>();
            }

            // 2. 사용자 선호 특성 프로필 생성
            UserPreferenceProfile profile = buildUserPreferenceProfile(userLikedBooks);

            // 3. 모든 도서와 선호 프로필 유사도 계산
            List<Book> candidateBooks = bookRepository.findAvailableBooks();
            Set<Integer> readBookIds = userLikedBooks.stream()
                .map(Book::getId).collect(Collectors.toSet());

            Map<Integer, Double> bookScores = new HashMap<>();
            for (Book book : candidateBooks) {
                // 이미 읽은 도서는 제외
                if (!readBookIds.contains(book.getId())) {
                    double similarity = calculateContentSimilarity(book, profile);
                    if (similarity > MIN_SIMILARITY_THRESHOLD) {
                        bookScores.put(book.getId(), similarity);
                    }
                }
            }

            // 4. 점수 순으로 정렬하여 추천 결과 생성
            return bookScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(count)
                .map(entry -> {
                    Book book = bookRepository.findById(entry.getKey()).orElse(null);
                    if (book != null) {
                        String reason = generateContentBasedReason(book, profile);
                        return RecommendationResult.builder()
                            .bookId(entry.getKey())
                            .title(book.getTitle())
                            .author(book.getAuthorName())
                            .score(entry.getValue())
                            .reason(reason)
                            .algorithm("content_based")
                            .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("콘텐츠 기반 추천 실패 - 사용자: {}, 오류: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 인기도 기반 추천
     * 
     * 사용 목적:
     * - 신규 사용자 콜드 스타트 문제 해결
     * - 추천의 다양성 확보
     * - 트렌드 반영
     * 
     * 인기도 계산 요소:
     * - 대여 횟수
     * - 평점 평균
     * - 최근성 (시간 가중치)
     * - 사용자 참여도 (리뷰, 평점 수)
     */
    private List<RecommendationResult> getPopularityBasedRecommendations(Integer userId, int count) {
        try {
            log.debug("인기도 기반 추천 시작 - 사용자: {}", userId);

            // 1. 사용자가 읽지 않은 인기 도서 조회
            Set<Integer> userReadBooks = getUserReadBookIds(userId);
            
            // 2. Redis에서 인기 도서 목록 조회
            List<String> popularBookIds = redisCacheService.getPopularBooks(count * 2); // 여유분 확보
            
            List<RecommendationResult> results = new ArrayList<>();
            for (String bookIdStr : popularBookIds) {
                try {
                    Integer bookId = Integer.parseInt(bookIdStr);
                    
                    // 이미 읽은 도서는 제외
                    if (!userReadBooks.contains(bookId)) {
                        Book book = bookRepository.findById(bookId).orElse(null);
                        if (book != null) {
                            double popularityScore = calculatePopularityScore(book);
                            
                            results.add(RecommendationResult.builder()
                                .bookId(bookId)
                                .title(book.getTitle())
                                .author(book.getAuthorName())
                                .score(popularityScore)
                                .reason("많은 사용자들이 선택한 인기 도서")
                                .algorithm("popularity_based")
                                .build());
                                
                            if (results.size() >= count) {
                                break;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("잘못된 도서 ID 형식: {}", bookIdStr);
                }
            }

            return results;

        } catch (Exception e) {
            log.error("인기도 기반 추천 실패 - 사용자: {}, 오류: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 실시간 추천 업데이트 (사용자 행동 기반)
     * 
     * 트리거 이벤트:
     * - 도서 평점/리뷰 작성
     * - 도서 대여/반납
     * - 검색/조회 패턴 변화
     * - 소셜 활동 (좋아요, 공유)
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> updateRecommendationsOnUserAction(
            Integer userId, String actionType, Integer bookId, Object actionData) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("사용자 행동 기반 추천 업데이트 - 사용자: {}, 액션: {}, 도서: {}", 
                    userId, actionType, bookId);

                // 1. 기존 추천 캐시 무효화
                invalidateRecommendationCache(userId);

                // 2. 액션 타입별 가중치 업데이트
                switch (actionType.toLowerCase()) {
                    case "rating":
                        updateUserPreferenceOnRating(userId, bookId, actionData);
                        break;
                    case "borrow":
                        updateUserPreferenceOnBorrow(userId, bookId);
                        break;
                    case "view":
                        updateUserPreferenceOnView(userId, bookId, actionData);
                        break;
                    case "search":
                        updateUserPreferenceOnSearch(userId, actionData);
                        break;
                    default:
                        log.debug("추천 업데이트가 필요없는 액션: {}", actionType);
                }

                // 3. 신규 추천 사전 계산 (백그라운드)
                getPersonalizedRecommendations(userId, DEFAULT_RECOMMENDATION_COUNT);

                log.debug("추천 업데이트 완료 - 사용자: {}, 액션: {}", userId, actionType);

            } catch (Exception e) {
                log.error("추천 업데이트 실패 - 사용자: {}, 액션: {}, 오류: {}", 
                    userId, actionType, e.getMessage(), e);
            }
        });
    }

    /**
     * 배치 추천 재계산 (스케줄링)
     * 
     * 수행 작업:
     * - 모든 사용자 추천 재계산
     * - 유사도 매트릭스 업데이트
     * - 인기도 점수 갱신
     * - 성능 메트릭 수집
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> batchUpdateRecommendations() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("배치 추천 재계산 시작");
                long startTime = System.currentTimeMillis();

                // 1. 활성 사용자 목록 조회
                List<Integer> activeUserIds = getActiveUserIds();
                
                int processedCount = 0;
                int successCount = 0;

                // 2. 사용자별 추천 재계산
                for (Integer userId : activeUserIds) {
                    try {
                        invalidateRecommendationCache(userId);
                        getPersonalizedRecommendations(userId, DEFAULT_RECOMMENDATION_COUNT);
                        successCount++;
                        
                        // 시스템 부하 방지를 위한 지연
                        Thread.sleep(100);
                        
                    } catch (Exception e) {
                        log.warn("사용자 추천 재계산 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
                    }
                    
                    processedCount++;
                    
                    // 진행률 로깅 (1000명마다)
                    if (processedCount % 1000 == 0) {
                        log.info("배치 추천 진행률 - 처리: {}/{}, 성공: {}", 
                            processedCount, activeUserIds.size(), successCount);
                    }
                }

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                log.info("배치 추천 재계산 완료 - 처리: {}, 성공: {}, 소요시간: {}ms", 
                    processedCount, successCount, duration);

                // 3. 성능 메트릭 수집
                recordBatchMetrics(processedCount, successCount, duration);

            } catch (Exception e) {
                log.error("배치 추천 재계산 실패: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 헬퍼 메서드들
     */
    
    private Map<Integer, Double> getUserRatings(Integer userId) {
        // 사용자의 도서 평점 맵 반환 (스텁 구현)
        return new HashMap<>();
    }

    private List<UserSimilarity> findSimilarUsers(Integer userId, Map<Integer, Double> userRatings) {
        // 유사한 사용자 목록 반환 (스텁 구현)
        return new ArrayList<>();
    }

    private List<Book> getUserLikedBooks(Integer userId) {
        // 사용자가 좋아한 도서 목록 반환 (평점 4.0 이상, 대여 이력 등)
        return new ArrayList<>();
    }

    private UserPreferenceProfile buildUserPreferenceProfile(List<Book> books) {
        // 사용자 선호 프로필 생성 (스텁 구현)
        return new UserPreferenceProfile();
    }

    private double calculateContentSimilarity(Book book, UserPreferenceProfile profile) {
        // 콘텐츠 유사도 계산 (스텁 구현)
        return 0.5;
    }

    private String generateContentBasedReason(Book book, UserPreferenceProfile profile) {
        // 추천 이유 생성 (스텁 구현)
        return "선호 장르와 비슷한 도서";
    }

    private Set<Integer> getUserReadBookIds(Integer userId) {
        // 사용자가 읽은 도서 ID 집합 반환 (스텁 구현)
        return new HashSet<>();
    }

    private double calculatePopularityScore(Book book) {
        // 인기도 점수 계산 (스텁 구현)
        return 5.0;
    }

    private List<RecommendationResult> deduplicateAndNormalize(List<RecommendationResult> recommendations, int count) {
        // 중복 제거 및 점수 정규화 (스텁 구현)
        return recommendations.stream()
            .collect(Collectors.toMap(
                RecommendationResult::getBookId,
                r -> r,
                (existing, replacement) -> existing.getScore() > replacement.getScore() ? existing : replacement))
            .values()
            .stream()
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(count)
            .collect(Collectors.toList());
    }

    private List<RecommendationResult> getCachedRecommendations(Integer userId) {
        // 캐시된 추천 조회 (스텁 구현)
        return null;
    }

    private void cacheRecommendations(Integer userId, List<RecommendationResult> recommendations) {
        // 추천 결과 캐싱 (스텁 구현)
        try {
            redisCacheService.trackUserActivity(userId, "recommendations_cached", 
                Map.of("count", recommendations.size(), "timestamp", LocalDateTime.now().toString()));
        } catch (Exception e) {
            log.warn("추천 캐싱 실패 - 사용자: {}", userId);
        }
    }

    private void invalidateRecommendationCache(Integer userId) {
        // 추천 캐시 무효화 (스텁 구현)
        log.debug("추천 캐시 무효화 - 사용자: {}", userId);
    }

    private void logRecommendationEvent(Integer userId, List<RecommendationResult> recommendations) {
        try {
            // 추천 이벤트 로깅 (분석용)
            Map<String, Object> eventData = Map.of(
                "userId", userId,
                "recommendationCount", recommendations.size(),
                "algorithms", recommendations.stream()
                    .map(RecommendationResult::getAlgorithm)
                    .collect(Collectors.toSet()),
                "timestamp", LocalDateTime.now()
            );
            
            log.info("추천 이벤트 로깅: {}", eventData);
            
        } catch (Exception e) {
            log.warn("추천 이벤트 로깅 실패: {}", e.getMessage());
        }
    }

    private void updateUserPreferenceOnRating(Integer userId, Integer bookId, Object ratingData) {
        // 평점 기반 선호도 업데이트 (스텁)
        log.debug("평점 기반 선호도 업데이트 - 사용자: {}, 도서: {}", userId, bookId);
    }

    private void updateUserPreferenceOnBorrow(Integer userId, Integer bookId) {
        // 대여 기반 선호도 업데이트 (스텁)
        log.debug("대여 기반 선호도 업데이트 - 사용자: {}, 도서: {}", userId, bookId);
    }

    private void updateUserPreferenceOnView(Integer userId, Integer bookId, Object viewData) {
        // 조회 기반 선호도 업데이트 (스텁)
        log.debug("조회 기반 선호도 업데이트 - 사용자: {}, 도서: {}", userId, bookId);
    }

    private void updateUserPreferenceOnSearch(Integer userId, Object searchData) {
        // 검색 기반 선호도 업데이트 (스텁)
        log.debug("검색 기반 선호도 업데이트 - 사용자: {}", userId);
    }

    private List<Integer> getActiveUserIds() {
        // 활성 사용자 ID 목록 반환 (최근 30일 활동)
        return List.of(1, 2, 3); // 스텁 구현
    }

    private void recordBatchMetrics(int processed, int success, long duration) {
        try {
            Map<String, Object> metrics = Map.of(
                "processedUsers", processed,
                "successfulUpdates", success,
                "durationMs", duration,
                "successRate", (double) success / processed * 100,
                "timestamp", LocalDateTime.now()
            );
            
            log.info("배치 추천 메트릭: {}", metrics);
            
        } catch (Exception e) {
            log.warn("배치 메트릭 기록 실패: {}", e.getMessage());
        }
    }

    /**
     * 내부 클래스들
     */
    
    public static class UserSimilarity {
        private final Integer userId;
        private final double similarity;
        
        public UserSimilarity(Integer userId, double similarity) {
            this.userId = userId;
            this.similarity = similarity;
        }
        
        public Integer getUserId() { return userId; }
        public double getSimilarity() { return similarity; }
    }
    
    public static class UserPreferenceProfile {
        private Map<String, Double> genreWeights = new HashMap<>();
        private Map<String, Double> authorWeights = new HashMap<>();
        private Map<String, Double> keywordWeights = new HashMap<>();
        
        // Getters and setters
        public Map<String, Double> getGenreWeights() { return genreWeights; }
        public Map<String, Double> getAuthorWeights() { return authorWeights; }
        public Map<String, Double> getKeywordWeights() { return keywordWeights; }
    }
}