package com.biglol.springsecuritypractice.admin;

import com.biglol.springsecuritypractice.user.User;
import com.biglol.springsecuritypractice.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 가짜 유저를 세팅하는 방법은 여기에서 3가지만 다룸
// 1. @WithMockUser, 2. @WithUserDetails, 3. ~.with

@SpringBootTest
@ActiveProfiles(profiles = "test")
@Transactional
class AdminControllerTest {
    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc; // mockMvc는 서버에 배포를 하지 않고도 spring mvc 동작을 재현할 수 있도록 도화줌
    // 서버의 띄우고 진짜로 요청하는 방식이 아님. 재현을 통한 동작

    private User user;

    private User admin;

    @BeforeEach
    public void setUp(@Autowired WebApplicationContext applicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity()) // spring security 적용
                .alwaysDo(print())
                .build();

        // ROLE_USER 권한이 있는 유저 생성
        user = userRepository.save(new User("user", "user", "ROLE_USeR"));
        // ROLE_ADMIN 권한이 있는 관리자 생성
        admin = userRepository.save(new User("admin", "admin", "ROLE_ADMIN"));
    }

    @Test
    void getNoteForAdmin_인증없음() throws Exception {
        mockMvc.perform(get("/admin").with(csrf())) // csrf 토큰 추가
                .andExpect(redirectedUrlPattern("**/login"))
                .andExpect(status().is3xxRedirection()); // login이 안되어있으므로 로그인 페이지로 redirect
    }

    @Test
    void getNoteForAdmin_어드민인증있음() throws Exception {
        mockMvc.perform(get("/admin").with(csrf()).with(user(admin))) // 어드민 추가. 직접 사용자를 mockMvc에 지정하는 방식
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void getNoteForAdmin_유저인증있음() throws Exception {
        mockMvc.perform(get("/admin").with(csrf()).with(user(user))) // 유저 추가
                .andExpect(status().isForbidden());
    }
}