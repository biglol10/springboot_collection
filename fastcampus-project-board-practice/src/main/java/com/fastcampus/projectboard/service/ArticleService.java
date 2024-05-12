package com.fastcampus.projectboard.service;

import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.domain.type.SearchType;
import com.fastcampus.projectboard.dto.ArticleDto;
import com.fastcampus.projectboard.dto.ArticleUpdateDto;
import com.fastcampus.projectboard.dto.ArticleWithCommentsDto;
import com.fastcampus.projectboard.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ArticleService {
    private final ArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public Page<ArticleDto> searchArticles(SearchType searchType, String searchKeyword, Pageable pageable) {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return articleRepository.findAll(pageable).map(ArticleDto::from);
        }

        return switch (searchType) {
            case TITLE -> articleRepository.findByTitleContaining(searchKeyword, pageable).map(ArticleDto::from);
            case CONTENT -> articleRepository.findByContentContaining(searchKeyword, pageable).map(ArticleDto::from);
            case ID -> articleRepository.findByUserAccount_UserIdContaining(searchKeyword, pageable).map(ArticleDto::from);
            case NICKNAME -> articleRepository.findByUserAccount_NicknameContaining(searchKeyword, pageable).map(ArticleDto::from);
            case HASHTAG -> articleRepository.findByHashtag("#" + searchKeyword, pageable).map(ArticleDto::from);
        };
//        return Page.empty();
    }

    @Transactional(readOnly = true)
    public ArticleWithCommentsDto getArticle(Long articleId) {
        return articleRepository.findById(articleId)
                .map(ArticleWithCommentsDto::from)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다 - articleId: " + articleId));
    }

    public void saveArticle(ArticleDto dto) {
        articleRepository.save(dto.toEntity());
    }

    public void updateArticle(ArticleDto dto) {
        // findById: 이 메서드는 데이터베이스에서 직접 엔티티를 조회합니다. 만약 해당 ID의 엔티티가 데이터베이스에 없다면 Optional.empty()를 반환합니다.
        // getReferenceById (또는 getOne): 이 메서드는 데이터베이스에서 엔티티를 직접 조회하는 대신 프록시 객체를 반환합니다. 프록시 객체는 실제 엔티티의 참조를 가지고 있지만, 실제 엔티티의 데이터는 처음에는 로드되지 않습니다. 프록시 객체의 메서드를 호출할 때 실제 엔티티의 데이터가 필요한 경우에만 데이터베이스에서 데이터를 로드합니다. 만약 해당 ID의 엔티티가 데이터베이스에 없다면 EntityNotFoundException을 발생시킵니다.
        // 따라서, getReferenceById는 엔티티의 데이터가 필요하지 않고 엔티티의 참조만 필요한 경우에 사용하면 효율적입니다. 반면, findById는 엔티티의 데이터가 즉시 필요한 경우에 사용합니다.
        try {
            Article article = articleRepository.getReferenceById(dto.id());
            if (dto.title() != null) {
                article.setTitle(dto.title());
            }
            if (dto.content() != null) {
                article.setContent(dto.content());
            }
            article.setHashtag(dto.hashtag());
//        articleRepository.save(article); // save를 호출하지 않아도 변경된 내용이 반영됨. Class level transaction에 의해 트랜잭션이 끝날 때 영속성 컨텍스트는 article이 변한 것을 감지해서 update 쿼리를 날림

        } catch (EntityNotFoundException e) {
            log.warn("게시글 업데이트 실패. 게시글을 찾을 수 없습니다 - dto: {}", dto);
//            throw new EntityNotFoundException("게시글이 없습니다 - articleId: " + dto.id());
        }

    }

    public void deleteArticle(long articleId) {
        articleRepository.deleteById(articleId);
    }

}
