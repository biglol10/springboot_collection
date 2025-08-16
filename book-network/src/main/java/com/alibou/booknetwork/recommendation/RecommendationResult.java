package com.alibou.booknetwork.recommendation;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 추천 결과 DTO
 * 
 * 추천 결과 데이터 모델의 설계 원칙:
 * 1. 투명성: 추천 이유 명시로 사용자 신뢰도 향상
 * 2. 추적성: 추천 알고리즘 및 성능 분석 지원
 * 3. 확장성: 다양한 추천 메타데이터 지원
 * 4. 성능: 최소한의 필수 정보로 네트워크 효율성
 * 
 * 추천 결과에 포함되는 정보:
 * - 기본 정보: 도서 ID, 제목, 저자
 * - 추천 품질: 신뢰도 점수, 순위
 * - 설명성: 추천 이유, 사용된 알고리즘
 * - 컨텍스트: 생성 시간, 유효 기간
 * - 메타데이터: 추가 분석 데이터
 * 
 * 사용자 경험 향상:
 * - 명확한 추천 이유 제공
 * - 개인화 수준 표시
 * - 대안 선택지 제공
 * - 피드백 수집 지원
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResult {

    /**
     * 추천된 도서 ID
     */
    private Integer bookId;

    /**
     * 도서 제목
     */
    private String title;

    /**
     * 저자명
     */
    private String author;

    /**
     * 추천 점수 (0.0 ~ 10.0)
     * 
     * 점수 의미:
     * - 9.0~10.0: 매우 높은 관련성 (강력 추천)
     * - 7.0~8.9: 높은 관련성 (추천)
     * - 5.0~6.9: 보통 관련성 (고려해볼 만함)
     * - 3.0~4.9: 낮은 관련성 (참고용)
     * - 0.0~2.9: 매우 낮은 관련성 (비추천)
     */
    private double score;

    /**
     * 추천 이유 (사용자 친화적 설명)
     * 
     * 예시:
     * - "비슷한 취향의 사용자들이 좋아한 도서"
     * - "최근 읽은 '해리포터'와 유사한 판타지 소설"
     * - "선호하는 '무라카미 하루키' 작가의 다른 작품"
     * - "많은 사용자들이 선택한 인기 도서"
     */
    private String reason;

    /**
     * 사용된 추천 알고리즘
     * 
     * 알고리즘 타입:
     * - collaborative_filtering: 협업 필터링
     * - content_based: 콘텐츠 기반
     * - popularity_based: 인기도 기반
     * - hybrid: 하이브리드
     * - trending: 트렌딩
     * - social: 소셜 추천
     */
    private String algorithm;

    /**
     * 추천 신뢰도 (0.0 ~ 1.0)
     * 
     * 신뢰도 계산 요소:
     * - 데이터 충분성 (사용자 이력, 도서 정보)
     * - 알고리즘 확신도
     * - 과거 추천 정확도
     * - 사용자 피드백 이력
     */
    @Builder.Default
    private double confidence = 0.0;

    /**
     * 추천 순위 (1부터 시작)
     */
    private int rank;

    /**
     * 도서 장르/카테고리
     */
    private String genre;

    /**
     * 도서 평점 평균
     */
    private Double averageRating;

    /**
     * 도서 대여 가능 여부
     */
    @Builder.Default
    private boolean available = true;

    /**
     * 예상 선호도 (사용자가 이 도서를 좋아할 확률)
     */
    private Double expectedPreference;

    /**
     * 다양성 점수 (기존 선호와 얼마나 다른지)
     * 
     * 다양성의 중요성:
     * - 필터 버블 방지
     * - 새로운 관심 분야 발견
     * - 장기적 사용자 만족도 향상
     */
    private Double diversityScore;

    /**
     * 신선도 점수 (얼마나 최신 정보 기반인지)
     */
    private Double freshnessScore;

    /**
     * 추천 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();

    /**
     * 추천 만료 시간 (이후로는 재계산 필요)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * 개인화 수준 (0.0~1.0)
     * 
     * 개인화 수준:
     * - 1.0: 완전 개인화 (사용자별 맞춤)
     * - 0.5: 부분 개인화 (그룹 기반)
     * - 0.0: 비개인화 (일반 인기)
     */
    @Builder.Default
    private double personalizationLevel = 0.0;

    /**
     * 추천 컨텍스트 (상황 정보)
     * 
     * 컨텍스트 예시:
     * - 시간대: 아침, 점심, 저녁, 주말
     * - 기분/상황: 휴식, 학습, 통근, 여행
     * - 날씨: 비오는 날, 화창한 날
     * - 이벤트: 휴가, 시험 기간, 연휴
     */
    private String context;

    /**
     * 추가 메타데이터 (JSON 형태의 확장 정보)
     */
    private Map<String, Object> metadata;

    /**
     * 사용자 피드백 여부 (추천을 클릭했는지)
     */
    @Builder.Default
    private boolean clicked = false;

    /**
     * 사용자 만족도 피드백 (1~5점)
     */
    private Integer userFeedback;

    /**
     * 추천 품질 등급
     */
    public RecommendationQuality getQuality() {
        if (score >= 8.0 && confidence >= 0.8) {
            return RecommendationQuality.EXCELLENT;
        } else if (score >= 6.0 && confidence >= 0.6) {
            return RecommendationQuality.GOOD;
        } else if (score >= 4.0 && confidence >= 0.4) {
            return RecommendationQuality.FAIR;
        } else {
            return RecommendationQuality.POOR;
        }
    }

    /**
     * 추천 만료 여부 확인
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 고신뢰도 추천 여부 확인
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    /**
     * 다양성 추천 여부 확인
     */
    public boolean isDiversityRecommendation() {
        return diversityScore != null && diversityScore >= 0.7;
    }

    /**
     * 개인화 추천 여부 확인
     */
    public boolean isPersonalized() {
        return personalizationLevel >= 0.5;
    }

    /**
     * 추천 요약 정보 반환 (로깅/분석용)
     */
    public String getSummary() {
        return String.format("도서: %s (ID: %d), 점수: %.2f, 알고리즘: %s, 신뢰도: %.2f", 
            title, bookId, score, algorithm, confidence);
    }

    /**
     * 사용자 표시용 점수 (별점 형태)
     */
    public double getStarRating() {
        return Math.round((score / 2.0) * 10.0) / 10.0; // 5점 만점으로 변환
    }

    /**
     * 추천 이유 카테고리 분류
     */
    public ReasonCategory getReasonCategory() {
        if (algorithm == null) {
            return ReasonCategory.UNKNOWN;
        }
        
        switch (algorithm.toLowerCase()) {
            case "collaborative_filtering":
                return ReasonCategory.SIMILAR_USERS;
            case "content_based":
                return ReasonCategory.SIMILAR_CONTENT;
            case "popularity_based":
                return ReasonCategory.TRENDING;
            case "social":
                return ReasonCategory.SOCIAL;
            default:
                return ReasonCategory.OTHER;
        }
    }

    /**
     * 추천 품질 등급
     */
    public enum RecommendationQuality {
        EXCELLENT("최고"),
        GOOD("좋음"),
        FAIR("보통"),
        POOR("낮음");
        
        private final String description;
        
        RecommendationQuality(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    /**
     * 추천 이유 카테고리
     */
    public enum ReasonCategory {
        SIMILAR_USERS("비슷한 사용자"),
        SIMILAR_CONTENT("유사한 콘텐츠"),
        TRENDING("인기/트렌드"),
        SOCIAL("소셜"),
        PERSONAL_HISTORY("개인 이력"),
        OTHER("기타"),
        UNKNOWN("알 수 없음");
        
        private final String description;
        
        ReasonCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    /**
     * 추천 결과 복사 (수정을 위한)
     */
    public RecommendationResult copy() {
        return RecommendationResult.builder()
            .bookId(this.bookId)
            .title(this.title)
            .author(this.author)
            .score(this.score)
            .reason(this.reason)
            .algorithm(this.algorithm)
            .confidence(this.confidence)
            .rank(this.rank)
            .genre(this.genre)
            .averageRating(this.averageRating)
            .available(this.available)
            .expectedPreference(this.expectedPreference)
            .diversityScore(this.diversityScore)
            .freshnessScore(this.freshnessScore)
            .generatedAt(this.generatedAt)
            .expiresAt(this.expiresAt)
            .personalizationLevel(this.personalizationLevel)
            .context(this.context)
            .metadata(this.metadata)
            .build();
    }

    /**
     * 추천 결과 검증
     */
    public boolean isValid() {
        return bookId != null && 
               title != null && !title.trim().isEmpty() &&
               score >= 0.0 && score <= 10.0 &&
               confidence >= 0.0 && confidence <= 1.0 &&
               algorithm != null && !algorithm.trim().isEmpty();
    }

    /**
     * 추천 메트릭 데이터 반환 (분석용)
     */
    public Map<String, Object> getMetrics() {
        return Map.of(
            "bookId", bookId,
            "score", score,
            "confidence", confidence,
            "algorithm", algorithm,
            "rank", rank,
            "personalizationLevel", personalizationLevel,
            "quality", getQuality().name(),
            "reasonCategory", getReasonCategory().name(),
            "generatedAt", generatedAt
        );
    }
}