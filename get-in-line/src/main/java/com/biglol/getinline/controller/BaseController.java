package com.biglol.getinline.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class BaseController implements ErrorController {
    @GetMapping("/")
    public String root() {
        return "index";
    }

    // spring boot에서 기본 동작으로 error controller 만들려고 하는데 여기에서 만든 error랑 충돌해서 에러가 발생
    // implements ErrorController 해줘야 함
    @RequestMapping("/error")
    public String error() {
        return "error";
    }
}
