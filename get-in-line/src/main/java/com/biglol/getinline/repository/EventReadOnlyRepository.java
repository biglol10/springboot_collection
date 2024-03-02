package com.biglol.getinline.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean // bean이 되지 않음
public interface EventReadOnlyRepository<T, ID> extends Repository<T, ID> {
    Optional<T> findById(ID id);

    Iterable<T> findAll();

    Iterable<T> findAllById(Iterable<ID> ids);

    Iterable<T> findAll(Sort sort);

    Page<T> findAll(Pageable pageable);
}
