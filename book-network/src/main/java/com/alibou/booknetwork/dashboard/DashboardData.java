package com.alibou.booknetwork.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 데이터 DTO
 * 
 * 대시보드 데이터 모델의 설계 원칙:
 * 1. 유연성: 다양한 대시보드 타입 지원
 * 2. 확장성: 새로운 메트릭 추가 용이
 * 3. 성능: 필요한 데이터만 선택적 포함
 * 4. 타입 안정성: 강타입 데이터 구조
 * 
 * 대시보드 타입별 데이터:
 * - admin: 전체 시스템 관리 데이터
 * - user: 개인 사용자 활동 데이터
 * - book: 도서별 분석 데이터
 * - analytics: 상세 분석 데이터
 * 
 * JSON 직렬화 최적화:
 * - 날짜 형식 통일
 * - null 값 제외
 * - 압축 가능한 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {

    /**
     * 대시보드 타입 (admin, user, book, analytics)
     */
    private String dashboardType;

    /**
     * 데이터 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 대상 사용자 ID (사용자 대시보드인 경우)
     */
    private Integer userId;

    /**
     * 대상 도서 ID (도서 대시보드인 경우)
     */
    private Integer bookId;

    /**
     * 오류 메시지 (조회 실패 시)
     */
    private String error;

    /**
     * 전체 개요 통계 (관리자 대시보드)
     */
    private OverviewStats overviewStats;

    /**
     * 실시간 활동 데이터
     */
    private RealtimeActivity realtimeActivity;

    /**
     * 핵심 성과 지표 (KPI)
     */
    private List<KPIMetric> kpiMetrics;

    /**
     * 트렌드 데이터
     */
    private TrendData trendData;

    /**
     * 인기 콘텐츠
     */
    private PopularContent popularContent;

    /**
     * 시스템 상태
     */
    private SystemStatus systemStatus;

    /**
     * 개인 통계 (사용자 대시보드)
     */
    private PersonalStats personalStats;

    /**
     * 읽기 활동 (사용자 대시보드)
     */
    private ReadingActivity readingActivity;

    /**
     * 소셜 활동 (사용자 대시보드)
     */
    private SocialActivity socialActivity;

    /**
     * 개인 추천 목록
     */
    private List<RecommendationSummary> recommendations;

    /**
     * 최근 활동 이벤트
     */
    private List<ActivityEvent> recentActivities;

    /**
     * 목표 진행률
     */
    private GoalProgress goalProgress;

    /**
     * 도서 통계 (도서 대시보드)
     */
    private BookStats bookStats;

    /**
     * 사용자 참여도 (도서 대시보드)
     */
    private UserEngagement userEngagement;

    /**
     * 평점 분석 (도서 대시보드)
     */
    private RatingAnalysis ratingAnalysis;

    /**
     * 대여 패턴 (도서 대시보드)
     */
    private BorrowPattern borrowPattern;

    /**
     * 대시보드 메타데이터
     */
    private Map<String, Object> metadata;

    /**
     * 대시보드 데이터 유효성 검증
     */
    public boolean isValid() {
        return dashboardType != null && timestamp != null && error == null;
    }

    /**
     * 캐시 가능 여부 확인
     */
    public boolean isCacheable() {
        return isValid() && error == null;
    }

    /**
     * 데이터 크기 추정 (바이트)
     */
    public long estimateSize() {
        // 대략적인 크기 계산 (실제로는 직렬화된 크기 측정)
        long size = 0;
        if (overviewStats != null) size += 500;
        if (realtimeActivity != null) size += 300;
        if (kpiMetrics != null) size += kpiMetrics.size() * 100;
        if (trendData != null) size += 1000;
        if (popularContent != null) size += 800;
        if (personalStats != null) size += 400;
        if (recommendations != null) size += recommendations.size() * 200;
        if (recentActivities != null) size += recentActivities.size() * 150;
        
        return size;
    }
}

/**
 * 전체 개요 통계
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class OverviewStats {
    private long totalUsers;
    private long totalBooks;
    private long totalBorrows;
    private long totalReviews;
    private int activeUsers;
    private int availableBooks;
    private int overdueBooks;
    
    /**
     * 사용자 증가율 (전월 대비)
     */
    private double userGrowthRate;
    
    /**
     * 도서 이용률
     */
    private double bookUtilizationRate;
}

/**
 * 실시간 활동 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class RealtimeActivity {
    private int currentOnlineUsers;
    private int todayNewUsers;
    private int todayBorrows;
    private int todayReturns;
    private int liveSearches;
    private int activeChats;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdate;
}

/**
 * 핵심 성과 지표 (KPI)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class KPIMetric {
    private String name;
    private double value;
    private double target;
    private String unit;
    private String trend; // up, down, stable
    private String description;
    private String alertLevel; // normal, warning, critical
    
    /**
     * 목표 달성률 계산
     */
    public double getAchievementRate() {
        return target > 0 ? (value / target) * 100 : 0;
    }
    
    /**
     * KPI 상태 확인
     */
    public String getStatus() {
        double rate = getAchievementRate();
        if (rate >= 100) return "excellent";
        if (rate >= 80) return "good";
        if (rate >= 60) return "fair";
        return "poor";
    }
}

/**
 * 트렌드 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TrendData {
    private int period; // 기간 (일)
    private Map<String, Integer> dailyUsers;
    private Map<String, Integer> dailyBorrows;
    private Map<String, Integer> dailyReviews;
    private Map<String, Integer> hourlyActivity;
    private Map<String, Integer> weeklyPattern;
}

/**
 * 인기 콘텐츠
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PopularContent {
    private List<String> topBooks;
    private List<String> topGenres;
    private List<String> topAuthors;
    private List<String> trendingSearches;
    private List<String> risingBooks;
}

/**
 * 시스템 상태
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SystemStatus {
    private String serverHealth; // healthy, degraded, down
    private String databaseStatus; // connected, slow, disconnected
    private String cacheStatus; // optimal, degraded, failed
    private double responseTime; // ms
    private double errorRate; // %
    private Map<String, Object> performanceMetrics;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdateTime;
    
    /**
     * 전체 시스템 상태 확인
     */
    public String getOverallStatus() {
        if ("down".equals(serverHealth) || "disconnected".equals(databaseStatus)) {
            return "critical";
        }
        if ("degraded".equals(serverHealth) || "slow".equals(databaseStatus) || errorRate > 5.0) {
            return "warning";
        }
        return "healthy";
    }
}

/**
 * 개인 통계
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PersonalStats {
    private int totalBooksRead;
    private int totalReviewsWritten;
    private double averageRating;
    private String favoriteGenre;
    private int readingStreak; // 연속 읽기 일수
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime memberSince;
}

/**
 * 읽기 활동
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ReadingActivity {
    private List<String> currentlyReading;
    private List<String> recentlyFinished;
    private Map<String, Integer> monthlyProgress;
    private double readingGoalProgress; // %
    private int pagesReadThisWeek;
    private double averageReadingTime; // 분
}

/**
 * 소셜 활동
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SocialActivity {
    private int followersCount;
    private int followingCount;
    private int likesReceived;
    private int commentsReceived;
    private int sharesCount;
    private int reviewLikesReceived;
}

/**
 * 추천 요약
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class RecommendationSummary {
    private Integer bookId;
    private String title;
    private String author;
    private double score;
    private String reason;
    private String imageUrl;
}

/**
 * 활동 이벤트
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ActivityEvent {
    private String type; // borrow, review, rating, social
    private String description;
    private String targetTitle; // 대상 도서/사용자 제목
    private String icon;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}

/**
 * 목표 진행률
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GoalProgress {
    private int yearlyGoal;
    private int currentProgress;
    private double goalAchievementRate; // %
    private int booksRemaining;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate estimatedCompletion;
    
    /**
     * 일일 평균 읽기 필요량 계산
     */
    public double getDailyReadingNeeded() {
        if (booksRemaining <= 0) return 0;
        
        LocalDate now = LocalDate.now();
        LocalDate yearEnd = LocalDate.of(now.getYear(), 12, 31);
        long daysRemaining = now.until(yearEnd).getDays();
        
        return daysRemaining > 0 ? (double) booksRemaining / daysRemaining : booksRemaining;
    }
}

/**
 * 도서 통계
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class BookStats {
    private int totalBorrows;
    private int totalReviews;
    private double averageRating;
    private int availableCopies;
    private int waitingList;
    private String popularityRank;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastBorrowed;
}

/**
 * 사용자 참여도
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UserEngagement {
    private double engagementScore; // 0-100
    private int uniqueBorrowers;
    private double averageReadingTime;
    private int socialInteractions; // 좋아요, 댓글, 공유 합계
    private int recommendationClicks;
    private Map<String, Integer> demographicBreakdown;
}

/**
 * 평점 분석
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class RatingAnalysis {
    private double averageRating;
    private int totalRatings;
    private Map<String, Integer> ratingDistribution; // 1-5점 분포
    private double sentimentScore; // 리뷰 감정 분석
    private List<String> commonKeywords; // 리뷰 키워드
    private String trendDirection; // improving, declining, stable
}

/**
 * 대여 패턴
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class BorrowPattern {
    private Map<String, Integer> hourlyPattern; // 시간대별 대여 패턴
    private Map<String, Integer> weeklyPattern; // 요일별 패턴
    private Map<String, Integer> monthlyTrend; // 월별 트렌드
    private int peakHour;
    private String peakDay;
    private double seasonalityIndex;
}

/**
 * 실시간 통계 (WebSocket/SSE용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class RealtimeStats {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private int activeUsers;
    private int currentBorrows;
    private int todayRegistrations;
    private int todayReviews;
    private Map<String, Object> systemLoad;
    private List<String> popularBooks;
    private List<String> recentActivities;
    private String error;
    
    /**
     * 실시간 상태 확인
     */
    public boolean isHealthy() {
        return error == null && activeUsers >= 0;
    }
}