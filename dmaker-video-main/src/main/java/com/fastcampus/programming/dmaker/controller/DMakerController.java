package com.fastcampus.programming.dmaker.controller;

import com.fastcampus.programming.dmaker.dto.*;
import com.fastcampus.programming.dmaker.exception.DMakerException;
import com.fastcampus.programming.dmaker.service.DMakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * @author Snow
 */

// DMakerController(Bean) DmakerService(Bean) DeveloperRepository(Bean)
// ================= Spring Application Context =======================

// 컨트롤러 단에서는 요청값을 잘 받아왔냐 아니냐 이 정도로만 체크하고
// 트랜잭션을 구분해줘야 하는 그런 케이스가 있거나 한 틀내잭션으로 끝날 수 없는 그런 로직이 있을 때만 컨트롤러 단에서 일부 로직이 들어갈 수 있음
// 컨트롤러 단에서 비즈니스 로직이 들어가면 안됨

@Slf4j
@RestController
@RequiredArgsConstructor
public class DMakerController {
    // 서비스 빈을 주입시켜야함
    // DMakerService를 여기다 넣어주는걸 자동으로 injection해준다
    private final DMakerService dMakerService;

    @GetMapping("/developers")
    public List<DeveloperDto> getAllDevelopers() {
        log.info("GET /developers HTTP/1.1");

        return dMakerService.getAllEmployedDevelopers();

//        return Collections.singletonList("Olaf"); // 단일 객체를 들고있는 list를 만들 때는 이렇게 해주는게 좋음. unmodifiablelist도 검색해보셈
    }

    @GetMapping("/developers2")
    public List<DeveloperDto> getAllDevelopers2() { // 엔티티와 우리가 응답에 내려주는 데이터를 서로 분리해주는게 좋음 (entity자체를 넘겨주면 안됨)
        return dMakerService.getAllEmployedDevelopers();
    }

    @GetMapping("/developers2/{memberId}")
    public DeveloperDetailDto getDeveloperDetail2(@PathVariable String memberId) {
        return dMakerService.getDeveloperDetail(memberId);
    }

    @GetMapping("/developer/{memberId}")
    public DeveloperDetailDto getDeveloperDetail(
            @PathVariable final String memberId
    ) {
        log.info("GET /developers HTTP/1.1");

        return dMakerService.getDeveloperDetail(memberId);
    }

    @PostMapping("/create-developer")
    public CreateDeveloper.Response createDevelopers(
            @Valid @RequestBody final CreateDeveloper.Request request
    ) {
        log.info("request : {}", request);

        return dMakerService.createDeveloper(request);
    }

    @PostMapping("/create-developer2")
    public CreateDeveloper2.Response createDevelopers2(@Valid @RequestBody CreateDeveloper2.Request request) {
        log.info("request: {}", request); // ToString annotation추가

        return dMakerService.createDeveloper2(request);
    }

    @PutMapping("/developer/{memberId}")
    public DeveloperDetailDto editDeveloper(
            @PathVariable final String memberId,
            @Valid @RequestBody final EditDeveloper.Request request
    ) {
        log.info("GET /developers HTTP/1.1");

        return dMakerService.editDeveloper(memberId, request);
    }

    @DeleteMapping("/developer/{memberId}")
    public DeveloperDetailDto deleteDeveloper(
            @PathVariable final String memberId
    ) {
        return dMakerService.deleteDeveloper(memberId);
    }

//    @ResponseStatus(value = HttpStatus.CONFLICT) // 안해주면 200ok가 뜸, 다만 200 ok로 내려주고 에러 코드와 에러 메시지쪽에서 정확한 에러의 종류나 에러의 원인에 대한 정보를 내려주는 쪽으로 개발하는게 더 많은 추세
//    @ExceptionHandler(DMakerException.class) // 컨트롤러에서 발생하는 dMekerException들을 핸들링 할 수 있는 function을 만들어 주는 것
//    // @ExceptionHandler(DMakerException.class) 해주면 이 컨트롤러 상에서 발생하는 DMakerException은 여기에서 바로 처리를 해 가지고 DMakerResponse라는 응답을 만들어주게 됨
//    public DMakerErrorResponse2 handleException (DMakerException e, HttpServletRequest request) { // ExceptionHandler은 현재 요청이 들어왔던 http survlet request를 함께 받을 수 있다
//        log.error("ErrorCode: {}, url: {}, message: {}", e.getDMakerErrorCode(), request.getRequestURI(), e.getDetailMessage());
//
//        return DMakerErrorResponse2.builder()
//                .errorCode(e.getDMakerErrorCode())
//                .errorMessage(e.getDetailMessage())
//                .build();
//    }
}
