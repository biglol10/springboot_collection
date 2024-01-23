package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.TodoModel;
import org.example.model.TodoRequest;
import org.example.services.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoController.class)
class TodoControllerTest2 {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    private TodoModel expected;

    @BeforeEach // 각 테스트가 실행되기 전마다 expected의 값을 초기화 용도
    void setup() {
        this.expected = new TodoModel();
        this.expected.setId(123L);
        this.expected.setTitle("test");
        this.expected.setOrder(0L);
        this.expected.setCompleted(false);
    }

    @Test
    void create() throws Exception {
        when(todoService.add(any(TodoRequest.class))) // This line tells Mockito to intercept calls to the add method of todoService. The any(TodoRequest.class) argument matcher indicates that this behavior should apply to any call to add that passes an instance of TodoRequest as its parameter, regardless of the specific details of that instance
                .then((i) -> {
                    TodoRequest request = i.getArgument(0, TodoRequest.class);
                    return new TodoModel(this.expected.getId(), request.getTitle(), request.getOrder(), request.getCompleted());
                });

        TodoRequest request = new TodoRequest();
        request.setTitle("ANY TITLE");

        // 이렇게 작성한 request를 request body에 넣어야 되는데 이 오브젝트 타입 자체로는 그게 안되기 때문에 ObjectMapper을 써서 body에 넣어줌
        ObjectMapper mapper = new ObjectMapper();
        String content = mapper.writeValueAsString(request); // request가 string으로 됨

        this.mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON)
                .content(content)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("ANY TITLE"));
    }

    @Test
    void readOne() {

    }
}