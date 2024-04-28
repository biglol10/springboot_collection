package com.fastcampus.projectboard.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@ToString
@Table(indexes = { // 다만 이게 AuditingFields로 옮기는건 안됨
        @Index(columnList = "title"),
        @Index(columnList = "hashtag"),
        @Index(columnList = "createdAt"),
        @Index(columnList = "createdBy")
})
//@EntityListeners(AuditingEntityListener.class)
@Entity
public class Article extends AuditingFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MYSQL autoincrement에 맞게
    private Long id;

    @Setter
    @Column(nullable = false)
    private String title;

    @Setter
    @Column(nullable = false, length = 10000)
    private String content;

    @Setter // @Column 없어도 됨 (Transient 언급이 없는 이상)
    private String hashtag;

    @ToString.Exclude // 순환참조문제 해결. 보통 이쪽에서 Exclude를 함. 이쪽에서 댓글 리스트를 다 뽑아 보는 거는 굳이 안봐도 됨
    @OrderBy("id")
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL) // article 테이블로부터 온 것이다. 모든 경우에 대해서 cascading constraint를 적용
    private final Set<ArticleComment> articleComments = new LinkedHashSet<>();

    // 아래 항목들 공통으로 옮김. 성향에 따라 공통 또는 개별로 작성
//    @CreatedDate
//    @Column(nullable = false)
//    private LocalDateTime createdAt; // 자동으로 세팅하게 해주려면 JPA Auditing 사용
//
//    @CreatedBy
//    @Column(nullable = false, length = 100)
//    private String createdBy;
//
//    @LastModifiedDate
//    @Column(nullable = false)
//    private LocalDateTime modifiedAt;
//
//    @LastModifiedBy
//    @Column(nullable = false, length = 100)
//    private String modifiedBy;

    // 모든 JPA Entity들은 Hibernate 구현체를 사용하는 경우를 기준으로 기본 생성자를 가지고 있어야 함
    protected Article() {}

    // private으로 막고 factory method를 통해서 제공할 수 있게끔 해봄
    private Article(String title, String content, String hashtag) {
        this.title = title;
        this.content = content;
        this.hashtag = hashtag;
    }

    // 도메인 article을 생성하고자 할 때 어떤 값을 필요로 한다는걸 이것으로 가이드
    public static Article of(String title, String content, String hashtag) {
        return new Article(title, content, hashtag);
    }

    // 리스트에 넣거나 리스트에 있는 중복 요소를 제거하거나 정렬 등의 케이스를 위해 동일성/동등성 비교를 하기 위해 equals and hashcode를 구현해야 함
    // lombok의 @EqualsAndHashCode를 쓸 수 있지만 모든 항목들을 다 비교함


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article article)) return false;
        return id != null && id.equals(article.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
