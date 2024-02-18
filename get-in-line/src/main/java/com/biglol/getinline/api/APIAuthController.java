package com.biglol.getinline.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// RestController은 Controller에 Response Body가 추가된 annotation
// 출력 결과물을 그대로 내보내줌
@RequestMapping("/api")
@RestController
public class APIAuthController {
    @GetMapping("/sign-up")
    public String signUp() {
        return "done";
    }

    @GetMapping("/login")
    public String login() {
        return "done";
    }
}
