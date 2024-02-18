*********

@Value annotation  
kebab-case only  
필드 주입 방식을 사용할 경우  
 - 인스턴스화 이후에 주입하므로, final을 쓸 수 없음
 - 생성자 안에서 보이지 않음

```agsl
@Component
public class MyBean {
    @Value("${name}")
    private String name;
}
```

그런데 이제 생성자 주입이 가능하니까 더이상 고민할 필요가 없음

*********

캐싱 사용법  
@EnableCaching - Configuration 클래스에 등록. 메인 application은 @SpringBootApplication annotation안에서 Configuration 클래스로 정리되어 있기에 가능.
아니면 따로 클래스를 만들어서 그걸 configuration클래스로 등록한 다음에 거기에다 enableCaching적용  
이후 @Cachable("키값")으로 적용  
이렇게 하면
```agsl
@Cachable("student")
public Student getStudent(String name) { return name }
```
일 때 key값이 student::name이렇게 됨  

Spring Cache는 컨테이너에서 모든 빈들이 완성이 된 뒤에 모두 로딩이 끝난 뒤에 활성화됨  
그런데 @PostConstruct의 정의는 모든 빈이 Spring 컨테이너에 등록된 뒤가 아니라 해당 클래스의 의존성이 모두 완성된 뒤임 (쓰는 쪽 보면 MyProperties랑 StudentService임)  
이 2개가 의존성 주입이 끝났다고 해서 그게 Spring 컨테이너에서 모든 빈을 로드했다라는 것과 맥락이 닿지는 않음. 그래서 cache가 @PostConstruct에서 동작하지 않음  
그래서 여기에선 EventListener 사용  

Redis의 경우  
build.gradle에 implementation 'org.springframework.boot:spring-boot-starter-data-redis'를 추가해줘야 함  
application.properties에 spring.cache.type=redis 추가

*********

