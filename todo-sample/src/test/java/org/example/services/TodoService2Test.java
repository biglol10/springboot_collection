package org.example.services;

import org.example.model.TodoModel;
import org.example.model.TodoRequest;
import org.example.repository.TodoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

// 테스트를 진행할 때 실제 db와 연결해서 하지는 않음, 테스트 할 때마다 데이터가 추가/수정될 수 있음
@ExtendWith(MockitoExtension.class) // mock 객체를 쓸 것이기 때문
class TodoService2Test {
    @Mock
    private TodoRepository todoRepository;

    @InjectMocks // mock을 주입받아서 사용할 Todoservice
    private TodoService2 todoService2;

    @Test
    void add() {
        when(this.todoRepository.save(any(TodoModel.class)))
                .then(AdditionalAnswers.returnsFirstArg()); // TodoEntity값을 받으면 받은 엔터티 값을 반환하도록 설정

        TodoRequest expected = new TodoRequest();
        expected.setTitle("Test Title");

        TodoModel actual = this.todoService2.add(expected);

        assertEquals(expected.getTitle(), actual.getTitle());

    }

    @Test
    void searchById() {
        TodoModel todoModel = new TodoModel();
        todoModel.setId(123L);
        todoModel.setTitle("TITLE");
        todoModel.setOrder(0L);
        todoModel.setCompleted(false);
        Optional<TodoModel> optional = Optional.of(todoModel);
        given(todoRepository.findById(anyLong()))
                .willReturn(optional); // 어떤 값이든 id값이 주어졌을 때 willReturn으로 optional값을 리턴하도록 함
        TodoModel actual = todoService2.searchById(123L);
        TodoModel expected = optional.get();

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getOrder(), actual.getOrder());
        assertEquals(expected.getCompleted(), actual.getCompleted());
    }

    @Test
    public void searchByIdFailed() {
        given(todoRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> {
            todoService2.searchById(123L);
        });


    }
}