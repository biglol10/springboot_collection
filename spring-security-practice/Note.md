*** Spring Security 내부구조 ***

- 더 자세한 노트는 여기에서 확인: https://github.com/ysiksik/ysiksik.github.io/blob/b51003f6f85185e535f03c90ac53a4c7958810c0/_posts/study/fast-campus/spring-complete-edition-super-gap-package-online/2023-01-03-Part5-Spring-Security.md

SecurityContextHolder -> SecurityContext -> Authentication -> Principal & GrantAuthority

![img.png](z_img/img.png)

![img.png](z_img/img2.png)

```java
SecurityContext securityContext = SecurityContextHolder.getContext(); // 해보면 securityContext.authentication.principal에 로그인 한 유자 정보가 담김
// User 엔티티의 정보가 담김
// 아무런 정보도 주지 않았는데 로그인 한 사람의 정보가 있는 이유는 ThreadLocal 덕분임
```
![img.png](z_img/img3.png)

![img.png](z_img/img4.png)

*** Security Filter ***  
요청이나 응답에 대해서 filtering 으로 감싸는 객체  
doFilter 메소드를 무조건 구현하도록 되어있음  

![img.png](z_img/img5.png)

![img.png](z_img/img6.png)

![img.png](z_img/img7.png)

stateless하다는건 상태를 저장하지 않고 매 요청마다 인증을 다시 한다는 뜻  
매번 보내야하기 때문에 노출의 위험성이 큼  
명시적으로 http.httpBasic().disable(); 하는게 좋음

![img.png](z_img/img8.png)

![img.png](z_img/img9.png)

DaoAuthenticationProvider에서 this.getUserDetailsService().loadUserByUsername 부분은 SecurityConfig부분의 userDetailsService은 호출해서 유저정보를 가져옴. 이후 유저 체크가 이루어짐

*** CsrfFilter ***

![img.png](z_img/img10.png)

![img.png](z_img/img11.png)

![img.png](z_img/img12.png)

thymeleaf를 쓰면 페이지를 만들 때 자동으로 csrf 토큰을 포함시켜줌.  (따로 추가하지 않아도 input type="hidden" name="_csrf" value="..."/>가 추가됨)

*** FilterSecurityInterceptor ***

![img.png](z_img/img13.png)

*** ExceptionTranslationFilter ***

![img.png](z_img/img14.png)

*** Spring Security Config ***

![img.png](z_img/img15.png)

![img.png](z_img/img16.png)

hasRole("ADMIN")에서 ROLE_가 빠져있는데 이건 기본으로 되어있음

![img.png](z_img/img17.png)

*** security test ***

![img.png](z_img/img18.png)

![img.png](z_img/img19.png)

![img.png](z_img/img20.png)

*** Session vs JWT ***

![img.png](z_img/img21.png)

![img.png](z_img/img22.png)

![img.png](z_img/img23.png)

*** JWT ***

![img.png](z_img/img24.png)

![img.png](z_img/img25.png)

![img.png](z_img/img26.png)

![img.png](z_img/img27.png)

![img.png](z_img/img28.png)

JwtKey는 JWT Secret Key를 관리하고 제공. Key Rolling (키 여러개 관리)를 지원  
JwtUtils - JWT 토큰을 생성하거나 Parsing하는 메소드를 제공  
SigningKeyResolver - JWT의 헤더에서 kid를 찾아서 Key(SecretKey+알고리즘)를 찾아옴. Signature검증할 때 사용
