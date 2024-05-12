package com.fastcampus.projectboard.repository;

import com.fastcampus.projectboard.config.JpaConfig;
import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.domain.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

// h2 inmemory db로 테스트. 만약에 mysql과 비슷한 환경으로 테스트하고 싶으면 application.yaml에서 testdb 참고 및 @ActiveProfiles("testdb") 넣음

//@ActiveProfiles("testdb") // 실제론 yaml의 testdb에 있는 내용들이 동작안함. jps test가 가동하는 순간 auto configuration test에서 바로 db 설정을 무시하고 자기가 미리 지정해놓은 테스트용 db띄워버림.
// 자동으로 테스트 db를 띄우지 못하게 막아줘야 함. @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)로 설정
// 이러면 테스트 상태에서 돌린다고 해도 테스트 db를 따로 불러오지 않고 설정되어 있는걸 씀. 
// 그런데 너무 장황해서 이걸 제거하고 test.database.replace: none로 수정 (해당 property는 문서화에 포함안되어 있어서 워닝이 뜰 수 있음)
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JPA 연결 테스트")
@Import(JpaConfig.class)
@DataJpaTest // slice 테스트. 그냥 이렇게 하면 테스트가 jpaconfig를 제대로 읽지 못함 (Auditing 기능) 그래서 Import(JpaConfig.class)넣음
class JpaRepositoryTest {
    private final ArticleRepository articleRepository; // @Autowired 써도 되지만 JUnit5에선 생성자에서 씀
    private final ArticleCommentRepository articleCommentRepository;
    private final UserAccountRepository userAccountRepository;

    public JpaRepositoryTest(@Autowired ArticleRepository articleRepository, @Autowired ArticleCommentRepository articleCommentRepository, @Autowired UserAccountRepository userAccountRepository) {
        this.articleRepository = articleRepository;
        this.articleCommentRepository = articleCommentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @DisplayName("Select 테스트")
    @Test
    void givenTestData_whenSelecting_thenWorksFind() {
        // Given

        // When
        List<Article> articles = articleRepository.findAll();
        List<String> aa = new LinkedList<>();
        System.out.println("articles is");
        System.out.println(articles.size());

        // Then
        assertThat(articles)
                .isNotNull()
                .hasSizeGreaterThan(0);
    }

    @DisplayName("Insert 테스트")
    @Test
    void givenTestData_whenInserting_thenWorksFine() {
        // Given
        long previousCount = articleRepository.count();
        UserAccount userAccount = userAccountRepository.save(UserAccount.of("uno", "pw", null, null, null));
        Article article = Article.of(userAccount, "new article", "new content", "#spring");

        // When
        articleRepository.save(article);

        // Then
        assertThat(articleRepository.count()).isEqualTo(previousCount + 1);
    }

    @DisplayName("Update 테스트")
    @Test
    void givenTestData_whenUpdating_thenWorksFine() {
        // Given
        Article article = articleRepository.findById(1L).orElseThrow();
        String updatedHashtag = "#springboot";
        article.setHashtag(updatedHashtag);

        // When
        // test의 내용이 롤백되기에 추가적인 작업을 하지않고 검증을 하고자 한다면 flush를 해줘야 함
        // save() 메소드는 바로 DB 에 저장되지 않고 영속성 컨텍스트에 저장되었다가 flush() 또는 commit() 수행 시 DB에 저장됨
        // saveAndFlush() 메소드는 즉시 DB 에 변경사항을 적용하는 방식
        Article savedArticle = articleRepository.saveAndFlush(article);

        // Then
        assertThat(savedArticle).hasFieldOrPropertyWithValue("hashtag", updatedHashtag);
    }

    @DisplayName("Delete 테스트")
    @Test
    void givenTestData_whenDeleting_thenWorksFine() {
        // Given
        Article article = articleRepository.findById(1L).orElseThrow();
        long previousArticleCount = articleRepository.count();
        long previousArticleCommentCount = articleCommentRepository.count();
        int deletedCommentsSize = article.getArticleComments().size();

        // When
        articleRepository.delete(article);

        // Then
        assertThat(articleRepository.count()).isEqualTo(previousArticleCount - 1);
        assertThat(articleCommentRepository.count()).isEqualTo(previousArticleCommentCount - deletedCommentsSize);

    }
}