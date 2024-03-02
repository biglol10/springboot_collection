package com.biglol.getinline.controller.api;

import org.springframework.web.bind.annotation.*;

import com.biglol.getinline.dto.APIDataResponse;
import com.biglol.getinline.dto.AdminRequest;
import com.biglol.getinline.dto.LoginRequest;

// RestController은 Controller에 Response Body가 추가된 annotation
// 출력 결과물을 그대로 내보내줌
/**
 * Spring Data REST 로 API 를 만들어서 당장 필요가 없어진 컨트롤러. 우선 deprecated 하고, 향후 사용 방안을 고민해 본다. 필요에 따라서는 다시 살릴
 * 수도 있음
 *
 * @deprecated 0.1.2
 */
@Deprecated
// @RequestMapping("/api")
// @RestController
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
