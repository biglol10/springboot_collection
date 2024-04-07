package com.biglol.springsecuritypractice.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
