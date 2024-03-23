package com.biglol.getinline.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.biglol.getinline.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL) // 이렇게 하면 @Autowired안 써도 됨.
// 생성자에 있는 모드 메소드 파라미터는 전부 다 Spring Container가 주도권을 가져감, 그래서 Autowired를 무조건 하려고 시도함. 그러나 비추천
// @AutoConfigureMockMvc
// @SpringBootTest
//@WebMvcTest(
//        BaseController
//                .class) // AutoConfigureMockMvc, SpringBootTest를 이용해서 AutoConfigureMockMvc가 MockMvc를
//// 만들어주는거임. 더 간단하게 컨트롤러 테스트만 하는 방법. 이렇게 되면 WebMvc가 모든 컨트롤러를 읽는데 그게 싫으면 이 안에서
//// 테스트하고 싶은 대상 컨트롤러만 가져옴

@DisplayName("View 컨트롤러 - 기본 페이지")
@WebMvcTest(
        controllers = BaseController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
class BaseControllerTest {

    //    @Autowired
    //    private MockMvc mvc; // mvc에 대한 설정 인스턴스를 만들고 설정을 직접 해줘야 하는데 이걸 자동으로 해주는게
    // AutoConfigureMockMvc. Autowire로 바로 주입받을 수 있음

    // 위 방식은 jUnit4까지는 많이 썼음
    // 사용하고 싶었던 어떤 dependency를 field 주입 방식으로 field를 만들고 autowired를 붙여서 많이 사용했음
    // 그러나 jUnit4까지는 생성자 주입이나 각 유닛 테스트에 입력 인자를 디자인해 줄 수가 없었음
    // 5부터는 가능해짐. basePageShouldIndexPage(@Autowired MockMvc mvc) & private final MockMvc mvc;
    // public BaseControllerTest(@Autowired MockMvc mvc)

    private final MockMvc mvc;

    //    @Autowired // 이렇게 하면 전체에 적용. 여러개의 dependency를 넣고자 할 때 이게 더 편리할 수 있음
    public BaseControllerTest(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    //    @BeforeEach // 테스트가 시행될 때마다 직전에 수행될 동작
    //    void setUp() {
    //    }
    //
    //    @AfterEach // 테스트가 시행될 때마다 직후에 수행될 동작
    //    void tearDown() {
    //    }

    @DisplayName("[view][GET] 기본 페이지 요청")
    @Test
    void basePageShouldIndexPage() throws Exception { // basePageShouldIndexPageWhenSmth,
        // givenNothing_whenRequestRootPage_thenReturnsIndexPage
        // Given

        // // When
        //        ResultActions result = mvc.perform(get("/"));

        // When & Then
        // get 입력 후 ctrl + enter (MockMvcRequestBuilders의 메소드 호출), 이후 option + enter (static import)
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "This is default page"))) // body 전체 검사가 아닌 부분만 검사
                .andExpect(view().name("index"))
                .andDo(print()); // 에러가 나면 출력해주고 에러가 안 나면 출력 안 해주는데 이걸 쓰면 에러 여부에 상관없이 무조건 볼 수 있어서
        // 초기엔 유용, 나중엔 제거

        // text/html;charset=UTF-8 에서 charset=UTF-8은 현대 브라우저들이 웹표준을 준수하기 때문에 이런 인코딩 디코딩 하는데 문제가 없어서
        // 더이상 사용되지 않을 것이기 때문에 뺌
        // andExpect(content().contentType(MediaType.TEXT_HTML))로 하면 text/html !=
        // text/html;charset=UTF-8일 수 있기에 contentTypeCompatibleWith 사용
    }
}
