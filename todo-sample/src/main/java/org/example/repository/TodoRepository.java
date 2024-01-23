package org.example.repository;

import org.example.model.TodoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TodoRepository extends JpaRepository<TodoModel, Long> {  // 1 - db와 연결될 격체, 2 - 객체의 id에 해당하는 필드
}
