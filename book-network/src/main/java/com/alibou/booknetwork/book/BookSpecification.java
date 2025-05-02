package com.alibou.booknetwork.book;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 도서 검색 조건 명세 클래스
 * 
 * 이 클래스는 Spring Data JPA의 Specification 패턴을 사용하여
 * 도서 엔티티에 대한 동적 쿼리를 생성하는 유틸리티 메소드를 제공합니다.
 * 
 * Specification 패턴을 통해 다양한 검색 조건을 조합하여 복잡한 쿼리를
 * 타입 안전하게 구성할 수 있으며, 이를 통해 동적인 도서 검색 기능을 구현합니다.
 * 
 * 사용 예시:
 * ```
 * Specification<Book> spec = BookSpecification.withOwnerId(userId)
 *                            .and(BookSpecification.titleContains(keyword))
 *                            .and(BookSpecification.isShareable(true));
 * List<Book> books = bookRepository.findAll(spec);
 * ```
 */
public class BookSpecification {

    /**
     * 특정 소유자의 도서를 검색하는 명세를 반환합니다.
     * 
     * @param ownerId 도서 소유자의 ID
     * @return 소유자 ID와 일치하는 도서를 검색하는 명세
     */
    public static Specification<Book> withOwnerId(Integer ownerId) {
        return (root, query, criteriaBuilder) -> {
            if (ownerId == null) {
                return criteriaBuilder.conjunction(); // 항상 true를 반환하는 조건
            }
            return criteriaBuilder.equal(root.get("owner").get("id"), ownerId);
        };
    }

    /**
     * 제목에 특정 키워드가 포함된 도서를 검색하는 명세를 반환합니다.
     * 
     * @param keyword 검색할 키워드
     * @return 제목에 키워드가 포함된 도서를 검색하는 명세
     */
    public static Specification<Book> titleContains(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")),
                "%" + keyword.toLowerCase() + "%"
            );
        };
    }

    /**
     * 저자명에 특정 키워드가 포함된 도서를 검색하는 명세를 반환합니다.
     * 
     * @param authorName 검색할 저자명 키워드
     * @return 저자명에 키워드가 포함된 도서를 검색하는 명세
     */
    public static Specification<Book> authorContains(String authorName) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(authorName)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("authorName")),
                "%" + authorName.toLowerCase() + "%"
            );
        };
    }

    /**
     * ISBN으로 도서를 검색하는 명세를 반환합니다.
     * 
     * @param isbn 검색할 ISBN
     * @return ISBN과 일치하는 도서를 검색하는 명세
     */
    public static Specification<Book> withIsbn(String isbn) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(isbn)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("isbn"), isbn);
        };
    }

    /**
     * 공유 가능 여부에 따라 도서를 검색하는 명세를 반환합니다.
     * 
     * @param shareable 공유 가능 여부
     * @return 공유 가능 여부와 일치하는 도서를 검색하는 명세
     */
    public static Specification<Book> isShareable(Boolean shareable) {
        return (root, query, criteriaBuilder) -> {
            if (shareable == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("shareable"), shareable);
        };
    }

    /**
     * 보관 상태에 따라 도서를 검색하는 명세를 반환합니다.
     * 
     * @param archived 보관 상태 여부
     * @return 보관 상태와 일치하는 도서를 검색하는 명세
     */
    public static Specification<Book> isArchived(Boolean archived) {
        return (root, query, criteriaBuilder) -> {
            if (archived == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("archived"), archived);
        };
    }

    /**
     * 시놉시스에 특정 키워드가 포함된 도서를 검색하는 명세를 반환합니다.
     * 
     * @param keyword 검색할 키워드
     * @return 시놉시스에 키워드가 포함된 도서를 검색하는 명세
     */
    public static Specification<Book> synopsisContains(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("synopsis")),
                "%" + keyword.toLowerCase() + "%"
            );
        };
    }
    
    /**
     * 특정 기간 이후에 생성된 도서를 검색하는 명세를 반환합니다.
     * 
     * @param date 기준 날짜
     * @return 지정된 날짜 이후에 생성된 도서를 검색하는 명세
     */
    public static Specification<Book> createdAfter(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"), date);
        };
    }

    /**
     * 특정 기간 이전에 생성된 도서를 검색하는 명세를 반환합니다.
     * 
     * @param date 기준 날짜
     * @return 지정된 날짜 이전에 생성된 도서를 검색하는 명세
     */
    public static Specification<Book> createdBefore(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdDate"), date);
        };
    }

    /**
     * 여러 검색 조건을 조합한 복합 명세를 생성합니다.
     * 
     * @param searchRequest 검색 요청 객체
     * @return 여러 조건을 조합한 복합 명세
     */
    public static Specification<Book> buildSearchSpecification(BookSearchRequest searchRequest) {
        return Specification.where(withOwnerId(searchRequest.getOwnerId()))
                .and(titleContains(searchRequest.getTitle()))
                .and(authorContains(searchRequest.getAuthorName()))
                .and(withIsbn(searchRequest.getIsbn()))
                .and(isShareable(searchRequest.getShareable()))
                .and(isArchived(searchRequest.getArchived()))
                .and(synopsisContains(searchRequest.getSynopsis()));
    }
    
    /**
     * 키워드를 통한 통합 검색을 위한 명세를 생성합니다.
     * 제목, 저자명, ISBN, 시놉시스 등 여러 필드에서 키워드를 검색합니다.
     * 
     * @param keyword 검색 키워드
     * @return 여러 필드에 대한 OR 조건의 명세
     */
    public static Specification<Book> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }
            
            List<Predicate> predicates = new ArrayList<>();
            String pattern = "%" + keyword.toLowerCase() + "%";
            
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("authorName")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("isbn")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("synopsis")), pattern));
            
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 동적 정렬 지원:
 *    - 여러 필드 기준 정렬 추가
 *    - 정렬 방향 동적 지정 
 *    
 *    예시:
 *    public static Specification<Book> withSorting(String sortField, Sort.Direction direction) {
 *        return (root, query, criteriaBuilder) -> {
 *            query.orderBy(direction.isAscending() 
 *                ? criteriaBuilder.asc(root.get(sortField))
 *                : criteriaBuilder.desc(root.get(sortField)));
 *            return criteriaBuilder.conjunction();
 *        };
 *    }
 * 
 * 2. 서브쿼리 및 복잡한 조인:
 *    - 리뷰 평점 기준 검색
 *    - 대여 이력 기반 인기도 검색
 *    
 *    예시:
 *    public static Specification<Book> withMinimumRating(Double rating) {
 *        return (root, query, criteriaBuilder) -> {
 *            Subquery<Double> ratingSubquery = query.subquery(Double.class);
 *            Root<Book> subRoot = ratingSubquery.from(Book.class);
 *            Join<Book, Feedback> feedbacks = subRoot.join("feedbacks");
 *            ratingSubquery.select(criteriaBuilder.avg(feedbacks.get("note")))
 *                .where(criteriaBuilder.equal(subRoot, root));
 *            return criteriaBuilder.greaterThanOrEqualTo(ratingSubquery, rating);
 *        };
 *    }
 * 
 * 3. 페이징 및 검색 최적화:
 *    - 페이징 시 카운트 쿼리 최적화
 *    - 조인 쿼리의 성능 개선
 *    
 *    예시:
 *    public static void optimizeCountQuery(Specification<Book> spec) {
 *        return (root, query, criteriaBuilder) -> {
 *            if (query.getResultType() == Long.class) {
 *                root.join("feedbacks", JoinType.LEFT).on(criteriaBuilder.conjunction());
 *            }
 *            return spec.toPredicate(root, query, criteriaBuilder);
 *        };
 *    }
 * 
 * 4. 다국어 검색 지원:
 *    - 언어 설정에 따른 검색 방식 변경
 *    - 전문 검색(Full-Text Search) 통합
 *    
 *    예시:
 *    public static Specification<Book> fullTextSearch(String keyword, Locale locale) {
 *        return (root, query, criteriaBuilder) -> {
 *            if (dialect instanceof MySQLDialect) {
 *                return criteriaBuilder.greaterThan(
 *                    criteriaBuilder.function("match", Double.class, 
 *                        root.get("title"), root.get("synopsis"),  
 *                        criteriaBuilder.literal(keyword)),
 *                    0.0
 *                );
 *            }
 *            // 다른 데이터베이스를 위한 대체 구현
 *        };
 *    }
 * 
 * 5. 지리적 검색 구현:
 *    - 위치 기반 검색 (근처 소유자의 도서 등)
 *    - 거리 계산 및 정렬
 * 
 * 6. 캐싱 전략:
 *    - 자주 사용되는 쿼리 결과 캐싱
 *    - 캐시 무효화 전략
 * 
 * 7. 검색 쿼리 로깅 및 분석:
 *    - 사용자 검색 패턴 분석
 *    - 검색 성능 측정 및 개선
 * 
 * 8. 보안 향상:
 *    - 멀티테넌시 지원 (조직별 데이터 분리)
 *    - 동적 접근 제어 통합
 */

/**
 * 도서 검색 요청 정보를 담는 DTO 클래스
 * 
 * 여러 검색 조건을 한번에 전달하기 위한 객체입니다.
 * BookSpecification.buildSearchSpecification 메소드에서 사용됩니다.
 */
class BookSearchRequest {
    private Integer ownerId;
    private String title;
    private String authorName;
    private String isbn;
    private Boolean shareable;
    private Boolean archived;
    private String synopsis;
    
    // Getters and Setters
    public Integer getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    
    public String getIsbn() {
        return isbn;
    }
    
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
    
    public Boolean getShareable() {
        return shareable;
    }
    
    public void setShareable(Boolean shareable) {
        this.shareable = shareable;
    }
    
    public Boolean getArchived() {
        return archived;
    }
    
    public void setArchived(Boolean archived) {
        this.archived = archived;
    }
    
    public String getSynopsis() {
        return synopsis;
    }
    
    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }
}