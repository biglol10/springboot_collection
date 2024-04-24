package com.fastcampus.projectboard.repository;

import com.fastcampus.projectboard.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JPA 연결 테스트")
@Import(JpaConfig.class)
@DataJpaTest // slice 테스트. 그냥 이렇게 하면 테스트가 jpaconfig를 제대로 읽지 못함 (Auditing 기능) 그래서 Import(JpaConfig.class)넣음
class JpaRepositoryTest {
    private final ArticleRepository articleRepository; // @Autowired 써도 되지만 JUnit5에선 생성자에서 씀
    private final ArticleCommentRepository articleCommentRepository;

    public JpaRepositoryTest(@Autowired ArticleRepository articleRepository, @Autowired ArticleCommentRepository articleCommentRepository) {
        this.articleRepository = articleRepository;
        this.articleCommentRepository = articleCommentRepository;
    }

    @DisplayName("Select 테스트")
    @Test
    void givenTestData_whenSelecting_thenWorksFind() {
        
    }
}