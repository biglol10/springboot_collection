package com.fastcampus.programming.dmaker.controller;

import com.fastcampus.programming.dmaker.dto.*;
import com.fastcampus.programming.dmaker.service.DMakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

/**
 * @author Snow
 */

// DMakerController(Bean) DmakerService(Bean) DeveloperRepository(Bean)
// ================= Spring Application Context =======================

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
    public List<String> createDevelopers2(@Valid @RequestBody CreateDeveloper2.Request request) {
        log.info("request: {}", request); // ToString annotation추가

        return Collections.singletonList("Olaf");
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
}
