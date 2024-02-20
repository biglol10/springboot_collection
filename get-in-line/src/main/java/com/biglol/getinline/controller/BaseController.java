package com.biglol.getinline.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// @ControllerAdvice(basePackageClasses = BaseController.class) // 이 클래스가 담겨있는 패키지가 범위가 됨
@Controller
public class BaseController {
    @GetMapping("/")
    public String root() throws Exception {
        throw new Exception("테스트");
        //        return "index";
    }

    // spring boot에서 기본 동작으로 error controller 만들려고 하는데 여기에서 만든 error랑 충돌해서 에러가 발생
    // implements ErrorController 해줘야 함
    //    @RequestMapping("/error")
    //    public String error() {
    //        return "error";
    //    }
}
