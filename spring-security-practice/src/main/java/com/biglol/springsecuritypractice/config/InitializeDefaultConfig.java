package com.biglol.springsecuritypractice.config;

import com.biglol.springsecuritypractice.note.NoteService;
import com.biglol.springsecuritypractice.notice.NoticeService;
import com.biglol.springsecuritypractice.user.User;
import com.biglol.springsecuritypractice.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// h2로 서버를 실행하기에 종료하면 데이터가 모두 사라짐. 재시작 할 때마다 기본 사용자와 기본 어드민 등록. DB 쓰면 이거 지움

@Configuration
@RequiredArgsConstructor
@Profile(value = "!test") // test 에서는 제외
public class InitializeDefaultConfig {
    private final UserService userService;
    private final NoteService noteService;
    private final NoticeService noticeService;

    /**
     * 유저 등록, note 4개 등록
     */
    @Bean
    public void initializeDefaultUser() {
        User user = userService.signup("user", "user");
        noteService.saveNote(user, "테스트", "테스트입니다.");
        noteService.saveNote(user, "테스트2", "테스트2입니다.");
        noteService.saveNote(user, "테스트3", "테스트3입니다.");
        noteService.saveNote(user, "여름 여행계획", "여름 여행계획 작성중...");
    }

    /**
     * 어드민등록, 공지사항 2개 등록
     */
    @Bean
    public void initializeDefaultAdmin() {
        userService.signupAdmin("admin", "admin");
        noticeService.saveNotice("환영합니다.", "환영합니다 여러분");
        noticeService.saveNotice("노트 작성 방법 공지", "1. 회원가입\n2. 로그인\n3. 노트 작성\n4. 저장\n* 본인 외에는 게시글을 볼 수 없습니다.");
    }
}
