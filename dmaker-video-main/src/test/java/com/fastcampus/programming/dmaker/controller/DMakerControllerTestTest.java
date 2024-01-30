package com.fastcampus.programming.dmaker.controller;


import com.fastcampus.programming.dmaker.dto.DeveloperDto;
import com.fastcampus.programming.dmaker.service.DMakerService;
import com.fastcampus.programming.dmaker.type.DeveloperLevel;
import com.fastcampus.programming.dmaker.type.DeveloperSkillType;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DMakerController.class) // mockito랑 유사. 컨트롤러 테스트를 위한 기능들 제공
class DMakerControllerTestTest {
    @Autowired
    private MockMvc mockMvc; // 컨트롤러에 있는 메소드를 직접 호출하면 거기에 있는 파라미터들이 직접 거기에 바인딩되고 validation체크도 되고 이런 것들을 테스트할 수 없기 때문에 mockMvc를 이용해서 가상으로 호출

    @MockBean // 가짜 bean으로 등록
    private DMakerService dMakerService;

    protected MediaType contentType =
            new MediaType(MediaType.APPLICATION_JSON.getType(),
                    MediaType.APPLICATION_JSON.getSubtype(),
                    StandardCharsets.UTF_8);

    @Test
    void getAllDevelopers() throws Exception {
        DeveloperDto juniorDeveloperDto = DeveloperDto.builder()
                        .developerSkillType(DeveloperSkillType.BACK_END)
                        .developerLevel(DeveloperLevel.JUNIOR)
                        .memberId("memberId1").build();

        DeveloperDto seniorDeveloperDto = DeveloperDto.builder()
                .developerSkillType(DeveloperSkillType.FRONT_END)
                .developerLevel(DeveloperLevel.SENIOR)
                .memberId("memberId2").build();

        given(dMakerService.getAllEmployedDevelopers())
                .willReturn(Arrays.asList(juniorDeveloperDto, seniorDeveloperDto));

        // 가짜 mockMvc가 get으로 호출하고 contentsType은 보내는 것 받는 것 둘다 json, utf-8형식의 인코딩
        mockMvc.perform(get("/developers").contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.[0].developerSkillType",
                                is(DeveloperSkillType.BACK_END.name()))
                )
                .andExpect(
                        jsonPath("$.[0].developerLevel",
                                is(DeveloperLevel.JUNIOR.name()))
                )
                .andExpect(
                        jsonPath("$.[1].developerSkillType",
                                is(DeveloperSkillType.FRONT_END.name()))
                )
                .andExpect(
                        jsonPath("$.[1].developerLevel",
                                is(DeveloperLevel.SENIOR.name()))
                ).andDo(print()); // 요청이나 응답에 대한 내용들이 상세하게 출력됨
    }

}
