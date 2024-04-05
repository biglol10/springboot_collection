*** Spring Security 내부구조 ***

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

DaoAuthenticationProvider에서 this.getUserDetailsService().loadUserByUsername 부분은 SecurityConfig부분의 userDetailsService은 호출해서 유저정보를 가져옴