package com.biglol.springsecuritypractice.config;

import com.biglol.springsecuritypractice.filter.StopwatchFilter;
import com.biglol.springsecuritypractice.filter.TesterAuthenticationFilter;
import com.biglol.springsecuritypractice.user.User;
import com.biglol.springsecuritypractice.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

// WebSecurityConfigurerAdapter 개발자가 security설정을 쉽게 구현할 수 있도록 함 (필요에 따라 override)
// EnableWebSecurity는 WebSecurityConfigurerAdapter나 WebSecurity관련된 걸 상속받을 때 적어줘야 함

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {
    private final UserService userService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // stopwatch filter
        http.addFilterBefore(
                new StopwatchFilter(),
                WebAsyncManagerIntegrationFilter.class // WebAsyncManagerIntegrationFilter가 첫번째인데 얘보다 더 앞에 위치해야 되기에 addFilterBefore씀
        );

        // tester authentication filter
        http.addFilterBefore(
                new TesterAuthenticationFilter(this.authenticationManager()),
                UsernamePasswordAuthenticationFilter.class
        );

        // basic authentication
        http.httpBasic().disable(); // basic authentication filter 비활성화
//        http.httpBasic(); // basic authentication filter 활성화 -> 로그인을 거치지 않고 직접 입력 (예시: curl로 입력)해도 일회성으로 페이지를 불러올 수 있음
        // 로그인이라는 과정이 없어도 username, password 로그인 데이터를 base64로 인코딩해서 모든 요청에 포함해서 보내면 BasicAuthenticationFilter는 이걸 인증함
        // 그렇기 때문에 세션이 필요없고 요청이 올 때마다 인증이 이루어짐. (stateless)
        // csrf
        http.csrf();
        // remember-me
        http.rememberMe();
        // authorization
        http.authorizeRequests()
                // /와 /home은 모두에게 허용
                .antMatchers("/", "/home", "/signup").permitAll()
                // note 페이지를 USER 롤을 가진 유저에게만 허용
                .antMatchers("/note").hasRole("USER") // 개인 노트 페이지는 유저만 접근할 수 있도록
                .antMatchers("/admin").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET, "/notice").authenticated() // 인증 받았다면 공지사항 볼 수 있음
                .antMatchers(HttpMethod.POST, "/notice").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/notice").hasRole("ADMIN")
                .anyRequest().authenticated();

        // login
        http.formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .permitAll(); // 모두 허용

        // logout
        http.logout()
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // 로그아웃을 어디에다 요청해야 되는지 적어줌
                .logoutSuccessUrl("/");
    }

    // 아래 코드는 SpringSecurity에 포함되지 않기에 security filter가 실행되지 않음
    @Override
    public void configure(WebSecurity web) {
        // 정적 리소스 spring security 대상에서 제외
        // web.ignoring().antMatchers("/images/**", "/css/**"); // 아래 코드와 같은 코드
        web.ignoring().requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }

    /**
     * UserDetailsService 구현
     *
     * @return UserDetailsService
     */

    // 유저와 관련된 설정. Spring Security가 유저 관련 정보를 가져올 때 어떻게 가져와야 하는지 알 수 없음.
    // Spring 입장에선 User Entity가 뭔지 모름. 그래서 코드로 어떻게 하면 유저를 가져올 수 있는지 작성
    // Bean으로 등록하게 되면 spring security가 내부적으로 사용하게 됨
    // Spring Security에서 유저를 찾을 때 씀
    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userService.findByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException(username);
            }
            return user;
        };
    }
}
