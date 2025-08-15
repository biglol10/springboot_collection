package com.alibou.booknetwork.service.cache;

import com.alibou.booknetwork.book.Book;
import com.alibou.booknetwork.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 고급 캐싱 서비스
 * 
 * 엔터프라이즈 캐싱 전략의 핵심 원칙:
 * 1. Cache-Aside Pattern: 애플리케이션이 캐시를 직접 관리
 * 2. Write-Through vs Write-Behind: 쓰기 전략 선택
 * 3. TTL(Time To Live): 데이터 신선도 관리
 * 4. Cache Warming: 사전 데이터 로딩
 * 5. Eviction Policy: 메모리 관리 정책
 * 
 * SSGD에서 학습한 고급 패턴:
 * - Redis 데이터 타입별 활용 (String, Hash, List, Set, Sorted Set)
 * - 분산 락을 통한 동시성 제어
 * - Pipeline을 통한 성능 최적화
 * - Lua 스크립트를 통한 원자적 연산
 * 
 * 성능상의 이점:
 * - 데이터베이스 부하 감소 (90% 이상 캐시 히트율 목표)
 * - 응답 시간 향상 (밀리초 단위 응답)
 * - 확장성 향상 (수평적 확장 용이)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 캐시 키 접두사 상수 (네임스페이스 관리)
    private static final String BOOK_CACHE_PREFIX = "book:";
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String BOOK_LIST_PREFIX = "books:list:";
    private static final String POPULAR_BOOKS_KEY = "books:popular";
    private static final String USER_ACTIVITY_PREFIX = "user:activity:";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String SESSION_PREFIX = "session:";

    // TTL 상수 (만료 시간 관리)
    private static final Duration BOOK_CACHE_TTL = Duration.ofHours(2);
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration LIST_CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration POPULAR_CACHE_TTL = Duration.ofHours(1);
    private static final Duration SESSION_TTL = Duration.ofDays(7);

    /**
     * 도서 정보 캐싱 (단일 객체 캐싱 패턴)
     * 
     * Cache-Aside 패턴 적용:
     * 1. 캐시 조회 → 있으면 반환
     * 2. 없으면 데이터베이스 조회
     * 3. 조회 결과를 캐시에 저장 후 반환
     * 
     * 왜 필요한가:
     * - 도서 상세 조회는 빈번하게 발생하는 읽기 연산
     * - 도서 정보는 상대적으로 변경이 적음
     * - 데이터베이스 부하 감소 효과 큼
     */
    public void cacheBook(Book book) {
        String key = BOOK_CACHE_PREFIX + book.getId();
        
        try {
            redisTemplate.opsForValue().set(key, book, BOOK_CACHE_TTL);
            log.debug("도서 캐시 저장 완료 - ID: {}, 제목: {}", book.getId(), book.getTitle());
            
        } catch (Exception e) {
            log.error("도서 캐시 저장 실패 - ID: {}, 오류: {}", book.getId(), e.getMessage(), e);
            // 캐시 실패는 기능에 영향을 주지 않아야 함 (graceful degradation)
        }
    }

    public Optional<Book> getCachedBook(Integer bookId) {
        String key = BOOK_CACHE_PREFIX + bookId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("도서 캐시 히트 - ID: {}", bookId);
                return Optional.of((Book) cached);
            }
            
            log.debug("도서 캐시 미스 - ID: {}", bookId);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("도서 캐시 조회 실패 - ID: {}, 오류: {}", bookId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 인기 도서 랭킹 관리 (Sorted Set 활용)
     * 
     * Sorted Set의 장점:
     * - 점수(score) 기반 자동 정렬
     * - O(log N) 시간복잡도로 효율적인 삽입/조회
     * - 범위 조회 지원 (TOP 10, TOP 100 등)
     * 
     * 실시간 랭킹 시스템:
     * - 도서 조회/대여 시마다 점수 증가
     * - 주기적으로 점수 감소 (시간에 따른 가중치 감소)
     * - 실시간으로 인기 도서 순위 제공
     */
    public void incrementBookPopularity(Integer bookId, double score) {
        try {
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            zSetOps.incrementScore(POPULAR_BOOKS_KEY, bookId.toString(), score);
            
            // TTL 설정 (키가 새로 생성된 경우)
            redisTemplate.expire(POPULAR_BOOKS_KEY, POPULAR_CACHE_TTL);
            
            log.debug("도서 인기도 증가 - ID: {}, 점수: {}", bookId, score);
            
        } catch (Exception e) {
            log.error("도서 인기도 업데이트 실패 - ID: {}, 오류: {}", bookId, e.getMessage(), e);
        }
    }

    public List<String> getPopularBooks(int count) {
        try {
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            
            // 높은 점수부터 조회 (역순)
            Set<Object> popularBookIds = zSetOps.reverseRange(POPULAR_BOOKS_KEY, 0, count - 1);
            
            if (popularBookIds != null) {
                List<String> result = new ArrayList<>(popularBookIds.size());
                for (Object bookId : popularBookIds) {
                    result.add(bookId.toString());
                }
                
                log.debug("인기 도서 조회 완료 - 요청: {}, 결과: {}", count, result.size());
                return result;
            }
            
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("인기 도서 조회 실패 - 오류: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 사용자 세션 관리 (Hash 데이터 타입 활용)
     * 
     * Hash의 장점:
     * - 필드별 개별 업데이트 가능
     * - 메모리 효율적 (작은 hash는 ziplist 사용)
     * - 구조적 데이터 표현에 적합
     * 
     * 세션 관리의 중요성:
     * - 분산 환경에서 세션 공유
     * - 로드 밸런서 사용 시 sticky session 불필요
     * - 세션 만료 관리 자동화
     */
    public void createUserSession(String sessionId, User user, String ipAddress) {
        String key = SESSION_PREFIX + sessionId;
        
        try {
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("userId", user.getId());
            sessionData.put("email", user.getEmail());
            sessionData.put("fullName", user.getFullName());
            sessionData.put("ipAddress", ipAddress);
            sessionData.put("createdAt", LocalDateTime.now().toString());
            sessionData.put("lastAccessAt", LocalDateTime.now().toString());
            
            redisTemplate.opsForHash().putAll(key, sessionData);
            redisTemplate.expire(key, SESSION_TTL);
            
            log.info("사용자 세션 생성 - 세션ID: {}, 사용자: {}", sessionId, user.getEmail());
            
        } catch (Exception e) {
            log.error("세션 생성 실패 - 세션ID: {}, 오류: {}", sessionId, e.getMessage(), e);
        }
    }

    public void updateSessionLastAccess(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        
        try {
            redisTemplate.opsForHash().put(key, "lastAccessAt", LocalDateTime.now().toString());
            redisTemplate.expire(key, SESSION_TTL); // TTL 갱신
            
            log.debug("세션 접근 시간 업데이트 - 세션ID: {}", sessionId);
            
        } catch (Exception e) {
            log.error("세션 업데이트 실패 - 세션ID: {}, 오류: {}", sessionId, e.getMessage(), e);
        }
    }

    public Map<Object, Object> getSessionData(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        
        try {
            Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(key);
            
            if (!sessionData.isEmpty()) {
                log.debug("세션 데이터 조회 성공 - 세션ID: {}", sessionId);
                updateSessionLastAccess(sessionId); // 접근 시간 업데이트
                return sessionData;
            }
            
            log.debug("세션 데이터 없음 - 세션ID: {}", sessionId);
            return new HashMap<>();
            
        } catch (Exception e) {
            log.error("세션 조회 실패 - 세션ID: {}, 오류: {}", sessionId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * API 호출 속도 제한 (Rate Limiting)
     * 
     * 슬라이딩 윈도우 알고리즘:
     * - 정확한 비율 제한 (토큰 버킷보다 정밀)
     * - 시간 윈도우 내 요청 수 추적
     * - Redis의 EXPIRE를 활용한 자동 정리
     * 
     * 엔터프라이즈에서 중요한 이유:
     * - DoS 공격 방어
     * - API 남용 방지
     * - 시스템 자원 보호
     * - 공정한 자원 사용 보장
     */
    public boolean isRateLimited(String identifier, int maxRequests, Duration timeWindow) {
        String key = RATE_LIMIT_PREFIX + identifier;
        
        try {
            Long currentTime = System.currentTimeMillis();
            Long windowStart = currentTime - timeWindow.toMillis();
            
            // 시간 윈도우를 벗어난 기록 삭제
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
            
            // 현재 요청 수 확인
            Long requestCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);
            
            if (requestCount != null && requestCount >= maxRequests) {
                log.warn("속도 제한 발동 - 식별자: {}, 요청 수: {}/{}", 
                    identifier, requestCount, maxRequests);
                return true;
            }
            
            // 현재 요청 기록
            redisTemplate.opsForZSet().add(key, currentTime.toString(), currentTime);
            redisTemplate.expire(key, timeWindow.plusMinutes(1)); // 여유있게 TTL 설정
            
            log.debug("요청 허용 - 식별자: {}, 현재 요청 수: {}/{}", 
                identifier, requestCount != null ? requestCount + 1 : 1, maxRequests);
            return false;
            
        } catch (Exception e) {
            log.error("속도 제한 확인 실패 - 식별자: {}, 오류: {}", identifier, e.getMessage(), e);
            // 오류 시 요청 허용 (fail-open policy)
            return false;
        }
    }

    /**
     * 사용자 활동 추적 (List 데이터 타입 활용)
     * 
     * List의 특성 활용:
     * - LPUSH: 새로운 활동을 리스트 앞에 추가
     * - LTRIM: 리스트 크기 제한 (메모리 관리)
     * - LRANGE: 최근 N개 활동 조회
     * 
     * 비즈니스 가치:
     * - 사용자 행동 분석 데이터 수집
     * - 개인화 추천 시스템 기반 데이터
     * - 사용자 경험 개선을 위한 인사이트
     */
    public void trackUserActivity(Integer userId, String activity, Map<String, Object> details) {
        String key = USER_ACTIVITY_PREFIX + userId;
        
        try {
            Map<String, Object> activityRecord = new HashMap<>();
            activityRecord.put("activity", activity);
            activityRecord.put("timestamp", LocalDateTime.now().toString());
            activityRecord.put("details", details);
            
            String activityJson = objectMapper.writeValueAsString(activityRecord);
            
            // 최신 활동을 리스트 앞에 추가
            redisTemplate.opsForList().leftPush(key, activityJson);
            
            // 최근 100개 활동만 유지 (메모리 효율성)
            redisTemplate.opsForList().trim(key, 0, 99);
            
            // TTL 설정 (30일)
            redisTemplate.expire(key, Duration.ofDays(30));
            
            log.debug("사용자 활동 추적 - 사용자ID: {}, 활동: {}", userId, activity);
            
        } catch (Exception e) {
            log.error("사용자 활동 추적 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage(), e);
        }
    }

    public List<String> getUserRecentActivities(Integer userId, int count) {
        String key = USER_ACTIVITY_PREFIX + userId;
        
        try {
            List<Object> activities = redisTemplate.opsForList().range(key, 0, count - 1);
            
            if (activities != null) {
                List<String> result = new ArrayList<>();
                for (Object activity : activities) {
                    result.add(activity.toString());
                }
                
                log.debug("사용자 최근 활동 조회 - 사용자ID: {}, 조회 수: {}", userId, result.size());
                return result;
            }
            
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("사용자 활동 조회 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 캐시 무효화 (Cache Invalidation)
     * 
     * 캐시 무효화 전략:
     * 1. 개별 키 삭제: 특정 데이터 변경 시
     * 2. 패턴 기반 삭제: 관련 데이터 일괄 삭제
     * 3. 시간 기반 만료: TTL을 통한 자동 만료
     * 
     * 일관성 보장:
     * - 데이터 변경과 캐시 무효화의 순서 중요
     * - 트랜잭션 커밋 후 캐시 무효화
     * - 실패 시 재시도 또는 강제 만료
     */
    public void invalidateBookCache(Integer bookId) {
        String key = BOOK_CACHE_PREFIX + bookId;
        
        try {
            redisTemplate.delete(key);
            log.info("도서 캐시 무효화 완료 - ID: {}", bookId);
            
        } catch (Exception e) {
            log.error("도서 캐시 무효화 실패 - ID: {}, 오류: {}", bookId, e.getMessage(), e);
        }
    }

    public void invalidateUserSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        
        try {
            redisTemplate.delete(key);
            log.info("세션 무효화 완료 - 세션ID: {}", sessionId);
            
        } catch (Exception e) {
            log.error("세션 무효화 실패 - 세션ID: {}, 오류: {}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * 캐시 통계 및 모니터링
     * 
     * 운영 관점에서 중요한 메트릭:
     * - 캐시 히트율 (Cache Hit Ratio)
     * - 캐시 미스율 (Cache Miss Ratio)  
     * - 평균 응답 시간
     * - 메모리 사용률
     * - 만료된 키 수
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Redis INFO 명령어를 통한 통계 수집
            Properties info = redisTemplate.getConnectionFactory()
                .getConnection().info();
                
            stats.put("connected_clients", info.getProperty("connected_clients"));
            stats.put("used_memory", info.getProperty("used_memory_human"));
            stats.put("keyspace_hits", info.getProperty("keyspace_hits"));
            stats.put("keyspace_misses", info.getProperty("keyspace_misses"));
            
            // 히트율 계산
            long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
            long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
            double hitRatio = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;
            stats.put("hit_ratio_percentage", String.format("%.2f", hitRatio));
            
            log.debug("캐시 통계 조회 완료 - 히트율: {}%", String.format("%.2f", hitRatio));
            
        } catch (Exception e) {
            log.error("캐시 통계 조회 실패 - 오류: {}", e.getMessage(), e);
            stats.put("error", "통계 조회 실패: " + e.getMessage());
        }
        
        return stats;
    }

    /**
     * 캐시 워밍 (Cache Warming)
     * 
     * 사전 데이터 로딩을 통한 성능 최적화:
     * - 애플리케이션 시작 시 자주 사용되는 데이터 미리 로딩
     * - 첫 번째 사용자의 응답 시간 개선
     * - 캐시 미스로 인한 데이터베이스 부하 방지
     */
    public void warmupCache() {
        log.info("캐시 워밍 시작");
        
        try {
            // 예시: 인기 도서 데이터 사전 로딩
            // List<Book> popularBooks = bookRepository.findPopularBooks();
            // for (Book book : popularBooks) {
            //     cacheBook(book);
            // }
            
            log.info("캐시 워밍 완료");
            
        } catch (Exception e) {
            log.error("캐시 워밍 실패 - 오류: {}", e.getMessage(), e);
        }
    }
}