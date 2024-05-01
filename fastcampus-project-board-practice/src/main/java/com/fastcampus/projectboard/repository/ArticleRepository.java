package com.fastcampus.projectboard.repository;

import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.domain.QArticle;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.StringExpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface ArticleRepository extends
        JpaRepository<Article, Long>,
        QuerydslPredicateExecutor<Article>, // QuerydslPredicateExecutors는 엔티티 안에 있는 모든 필드에 대한 동적 쿼리를 생성할 수 있게 해줌
        QuerydslBinderCustomizer<QArticle> // querydslPredicateExecutor를 사용할 때, querydslBinderCustomizer를 사용하면 querydsl을 커스터마이징할 수 있음. Exact match 대신 contains, startsWith, endsWith 등을 사용할 수 있음
{
    @Override
    default void customize(QuerydslBindings bindings, QArticle root) { // 인터페이스라 구현할 수 없지만 자바8부터 default 메소드를 사용할 수 있음
        bindings.excludeUnlistedProperties(true); // listing 하지 않는 프로퍼티를 제외시킴
        bindings.including(root.title, root.hashtag, root.createdAt, root.createdBy, root.content);
//        bindings.bind(root.title).first(StringExpression::likeIgnoreCase); // like '${v}'
        bindings.bind(root.title).first(StringExpression::containsIgnoreCase);
        bindings.bind(root.content).first(StringExpression::containsIgnoreCase); // like '%${v}%'
        bindings.bind(root.hashtag).first(StringExpression::containsIgnoreCase);
        bindings.bind(root.createdAt).first(DateTimeExpression::eq); // 시 분 초까지 정확히 일치해야 하기 때문에 나중에 따로 작업
        bindings.bind(root.createdBy).first(StringExpression::containsIgnoreCase);
    }
}