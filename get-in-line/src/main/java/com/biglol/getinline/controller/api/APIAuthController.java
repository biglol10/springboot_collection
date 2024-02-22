package com.biglol.getinline.controller.api;

import org.springframework.web.bind.annotation.*;

import com.biglol.getinline.dto.APIDataResponse;
import com.biglol.getinline.dto.AdminRequest;
import com.biglol.getinline.dto.LoginRequest;

// RestController은 Controller에 Response Body가 추가된 annotation
// 출력 결과물을 그대로 내보내줌
@RequestMapping("/api")
@RestController
public class APIAuthController {
    @PostMapping("/sign-up")
    public APIDataResponse<String> signUp(@RequestBody AdminRequest adminRequest) {
        return APIDataResponse.empty();
    }

    @PostMapping("/login")
    public APIDataResponse<String> login(@RequestBody LoginRequest loginRequest) {
        return APIDataResponse.empty();
    }
}
