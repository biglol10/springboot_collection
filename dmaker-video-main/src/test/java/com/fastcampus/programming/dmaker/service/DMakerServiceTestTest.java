package com.fastcampus.programming.dmaker.service;


import com.fastcampus.programming.dmaker.code.StatusCode;
import com.fastcampus.programming.dmaker.dto.CreateDeveloper;
import com.fastcampus.programming.dmaker.dto.DeveloperDetailDto;
import com.fastcampus.programming.dmaker.dto.DeveloperDto;
import com.fastcampus.programming.dmaker.entity.Developer;
import com.fastcampus.programming.dmaker.repository.DeveloperRepository;
import com.fastcampus.programming.dmaker.repository.RetiredDeveloperRepository;
import com.fastcampus.programming.dmaker.type.DeveloperLevel;
import com.fastcampus.programming.dmaker.type.DeveloperSkillType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

//@SpringBootTest // 모든 빈을 띄워주는 것이기 때문에 삭제
@ExtendWith(MockitoExtension.class) // 단순 자바 테스트가 아니라 Mockito라는 외부 기능을 활용해 가지고 이 테스트를 진행하겠다라는 의미로 extend with
class DMakerServiceTestTest {
//    @Autowired  // to use mock
//    private DMakerService dMakerService; // = new DMakerService()로 할 필요없이 스프링에서 자동적으로 bean으로 등록함 (by SpringBootTest, Autowired)


    @Mock
    private DeveloperRepository developerRepository;

    @Mock // repository를 가상의 mock으로 DMaker 서비스 안에서 등록을 해주게 됨
    private RetiredDeveloperRepository retiredDeveloperRepository;

    @InjectMocks // DMaker 서비스라는 이름의 클래스를 생성할 때 2개의 mock을 자동적으로 넣어주게 됨
    private DMakerService dMakerService;

    @Test
    public void testSomething() {
        given(developerRepository.findByMemberId(anyString())) // 아무 문자열이나 넣어주면 우리는 이런 응답을 주게 mocking하겠다
                .willReturn(Optional.of(Developer.builder()
                        .developerLevel(DeveloperLevel.SENIOR)
                        .developerSkillType(DeveloperSkillType.FRONT_END)
                        .experienceYears(12)
                        .statusCode(StatusCode.EMPLOYED)
                        .name("name")
                        .age(12)
                        .build()
                ));

        DeveloperDetailDto developerDetail = dMakerService.getDeveloperDetail("memberId");

        assertEquals(DeveloperLevel.SENIOR, developerDetail.getDeveloperLevel());
    }
}
