package com.fastcampus.programming.dmaker.service;


import com.fastcampus.programming.dmaker.code.StatusCode;
import com.fastcampus.programming.dmaker.dto.CreateDeveloper;
import com.fastcampus.programming.dmaker.dto.DeveloperDetailDto;
import com.fastcampus.programming.dmaker.dto.DeveloperDto;
import com.fastcampus.programming.dmaker.entity.Developer;
import com.fastcampus.programming.dmaker.exception.DMakerErrorCode;
import com.fastcampus.programming.dmaker.exception.DMakerException;
import com.fastcampus.programming.dmaker.repository.DeveloperRepository;
import com.fastcampus.programming.dmaker.repository.RetiredDeveloperRepository;
import com.fastcampus.programming.dmaker.type.DeveloperLevel;
import com.fastcampus.programming.dmaker.type.DeveloperSkillType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static com.fastcampus.programming.dmaker.code.StatusCode.EMPLOYED;
import static com.fastcampus.programming.dmaker.constant.DMakerConstant.MIN_SENIOR_EXPERIENCE_YEARS;
import static com.fastcampus.programming.dmaker.type.DeveloperLevel.SENIOR;
import static com.fastcampus.programming.dmaker.type.DeveloperSkillType.FRONT_END;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    private final Developer defaultDeveloper = Developer.builder()
            .developerLevel(SENIOR)
            .developerSkillType(FRONT_END)
            .experienceYears(12)
            .statusCode(EMPLOYED)
            .name("name")
            .age(12)
            .build();

    private CreateDeveloper.Request getCreateRequest(
            DeveloperLevel developerLevel,
            DeveloperSkillType developerSkillType,
            Integer experienceYears
    ) {
        return CreateDeveloper.Request.builder()
                .developerLevel(developerLevel)
                .developerSkillType(developerSkillType)
                .experienceYears(experienceYears)
                .memberId("memberId")
                .name("name")
                .age(32)
                .build();
    }

    private final CreateDeveloper.Request defaultCreateRequest = CreateDeveloper.Request.builder()
            .developerLevel(SENIOR)
            .developerSkillType(FRONT_END)
            .experienceYears(12)
            .memberId("memberId")
            .name("name")
            .age(32)
            .build();

    @Test
    public void testSomething() {
        given(developerRepository.findByMemberId(anyString())) // 아무 문자열이나 넣어주면 우리는 이런 응답을 주게 mocking하겠다
                .willReturn(Optional.of(Developer.builder()
                        .developerLevel(SENIOR)
                        .developerSkillType(FRONT_END)
                        .experienceYears(12)
                        .statusCode(StatusCode.EMPLOYED)
                        .name("name")
                        .age(12)
                        .build()
                ));

        DeveloperDetailDto developerDetail = dMakerService.getDeveloperDetail("memberId");

        assertEquals(SENIOR, developerDetail.getDeveloperLevel());
    }

    @Test
    void createDeveloperTest_success() {
        // given
        CreateDeveloper.Request request = CreateDeveloper.Request.builder()
                .developerLevel(SENIOR)
                .developerSkillType(FRONT_END)
                .experienceYears(12)
                .memberId("memberId")
                .name("name")
                .age(32)
                .build();

        given(developerRepository.findByMemberId(anyString()))
                .willReturn(Optional.empty());
        given(developerRepository.save(any()))
                .willReturn(defaultDeveloper);

        // mockito에서 제공하는 기능 중 하나가 mock 객체가 뭔가 동작을 할 때 파라미터로 받은 값을 캡처해서 그 캡처한 값을 우리가 검증해 활용할 수 있게 해줌
        // save하게 되는 녀석을 캡처하기 위해서는 agrumentcaptor이 필요함
        // 실제로 db에 저장을 하게되는 데이터가 무엇인지 확인하고 싶을 때 또는
        // 외부 api로 호출을 날릴 때 그 외부 api로 날리는 호출이 데이터가 어떤 것이 날아가고 있는지 확인하고 싶을 때 captor활용해서 해당 db호출하는 메서드의 파라미터 혹은 외부 api로 연동 호출을 날려주는 그 메소드에 날아가는 파라미터의 데이터를 확인하고 싶을 때
        // 실제로 날아간 데이터들을 받아서 검증해볼 수 있다
        ArgumentCaptor<Developer> captor =
                ArgumentCaptor.forClass(Developer.class);

        // when
        CreateDeveloper.Response developer = dMakerService.createDeveloper(getCreateRequest(SENIOR, FRONT_END, MIN_SENIOR_EXPERIENCE_YEARS));

        // then
        verify(developerRepository, times(1)) // 특정 mock이 몇번 호출됐다
                .save(captor.capture()); // repo에 세이브할 때 넘어가는 파라미터를 직접 캡처링 할 수 있음, save는 developerRepository의 save

        Developer savedDeveloper = captor.getValue(); // 캡처된 데이터 확인
        assertEquals(SENIOR, savedDeveloper.getDeveloperLevel());
        assertEquals(FRONT_END, savedDeveloper.getDeveloperSkillType());
        assertEquals(12, savedDeveloper.getExperienceYears());
    }

    @Test
    void createDeveloperTest_failed_with_duplicated() {
        // given
        given(developerRepository.findByMemberId(anyString()))
                .willReturn(Optional.of(defaultDeveloper));

        // mockito에서 제공하는 기능 중 하나가 mock 객체가 뭔가 동작을 할 때 파라미터로 받은 값을 캡처해서 그 캡처한 값을 우리가 검증해 활용할 수 있게 해줌
        // save하게 되는 녀석을 캡처하기 위해서는 agrumentcaptor이 필요함
        ArgumentCaptor<Developer> captor =
                ArgumentCaptor.forClass(Developer.class);

        // when

        // test돌려봤을 때 X가 안 나오고 !가 나오면 assertions 전에 에러가 발생했다는 뜻임
        // then
        // 동작과 검증을 한번에 함
        DMakerException dMakerException = assertThrows(DMakerException.class, () -> dMakerService.createDeveloper(defaultCreateRequest));

        assertEquals(DMakerErrorCode.DUPLICATED_MEMBER_ID, dMakerException.getDMakerErrorCode());
    }
}
