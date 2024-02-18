package com.fastcampus.springbootpractice;

import com.fastcampus.springbootpractice.properties.MyProperties;
import com.fastcampus.springbootpractice.properties.MyProperties1;
import com.fastcampus.springbootpractice.service.StudentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@ConfigurationPropertiesScan // Configuration을 생략해버리면 Bean 스캐닝을 하지 못하니 에러 발생, configuration-property와 configuration-annotation중에 configuration-annotation을 분리할 수 있다
@SpringBootApplication
public class FastcampusSpringBootPracticeApplication {

    private final MyProperties myProperties;
    private final StudentService studentService;
    private final String username;
    private final String password;

    private final MyProperties1 myProperties1;

    @Value("${my.hello}")
    private String text; // 인스턴스가 된 뒤에 초기화하려고 하니까 final을 쓸 수 없음, 그러나 생성자 주입을 한다면 괜찮음

    public FastcampusSpringBootPracticeApplication(
            MyProperties myProperties,
            StudentService studentService,
            @Value("${spring.datasource.username:vault가}") String username,
            @Value("${spring.datasource.password:꺼져 있어요}") String password,
            MyProperties1 myProperties1) {
        this.myProperties = myProperties;
        this.studentService = studentService;
        this.username = username;
        this.password = password;
        this.myProperties1 = myProperties1;
    }

    public static void main(String[] args) {
        SpringApplication.run(FastcampusSpringBootPracticeApplication.class, args);

//        FastcampusSpringBootPracticeApplication app = new FastcampusSpringBootPracticeApplication();
//        app.abc();  // this will return "[Value]: null". 필드 주입했을 때 일어날 수 있는 문제
//        생성자 주입 방식이 아니라 필드 주입 방식일 때 인스턴스가 생성된 뒤에서야 값을 집어넣기 때문에 타이밍에 대한 주의 필요

    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            System.out.println("내 키는: " + myProperties.getHeight());
            studentService.printStudent("jack");
            studentService.printStudent("jack");
            studentService.printStudent("jack");
            studentService.printStudent("fred");
            studentService.printStudent("cassie");
            studentService.printStudent("cassie");
            System.out.println("user: " + username);
            System.out.println("pw: " + password);
        };
    }

    public void abc() {
        System.out.println("[Value]: " + text);
    }

}
