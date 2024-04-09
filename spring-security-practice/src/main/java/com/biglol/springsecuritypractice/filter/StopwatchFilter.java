package com.biglol.springsecuritypractice.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//### Custom Filter 만들기
//        + 프로젝트에 SpringSecurity를 포함시켜 개발하다 보면 SpringScurity에서 기본으로 제공하는 필터뿐만 아니라 개발자가 원하는 방식대로 동작하는 필터가 필요할때가 많다
//        + 이럴때 커스텀 필터를 구현하면 된다
//        + 커스텀 필터를 구현하기 위해서는 다른 필터와 마찬가지로 Filter Interface를 구현해야 한다
//        + 그러나 Filter Interface를 직접 구현하게 되면 중복실행 문제가 있다
//        + 대부분의 경우에는 OncePerRequestFilter를 구현하기를 권장한다

@Slf4j
public class StopwatchFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        StopWatch stopWatch = new StopWatch(request.getServletPath());
        stopWatch.start();
        filterChain.doFilter(request, response); // 체인을 통한 요청 전달: filterChain.doFilter()를 호출하여 다음 단계로 요청을 넘긴다. 요청이 다 끝날 때까지 기다리고 stopWatch.stop한다
        stopWatch.stop();
        log.info(stopWatch.shortSummary());
    }
}
