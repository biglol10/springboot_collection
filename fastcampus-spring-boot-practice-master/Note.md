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