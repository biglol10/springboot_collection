package com.fastcampus.projectboard.repository;

import com.fastcampus.projectboard.domain.ArticleComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
//@Repository 안해도 됨. JpaRepository의 구현체인 SimpleJpaRepository에 붙어있음
public interface ArticleCommentRepository extends JpaRepository<ArticleComment, Long> {
}