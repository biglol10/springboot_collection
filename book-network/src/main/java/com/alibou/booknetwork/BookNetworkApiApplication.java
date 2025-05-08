package com.alibou.booknetwork;

import com.alibou.booknetwork.book.Book;
import com.alibou.booknetwork.book.BookRepository;
import com.alibou.booknetwork.role.Role;
import com.alibou.booknetwork.role.RoleRepository;
import com.alibou.booknetwork.user.User;
import com.alibou.booknetwork.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware") // we need to tell spring what auditing entity listener to use (the bean named auditorAware that we created) (this is for created_by and updated_by fields)
@EnableAsync
public class BookNetworkApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookNetworkApiApplication.class, args);
	}

	// initialize the database with some data
	@Bean
	public CommandLineRunner runner(
			RoleRepository roleRepository,
			UserRepository userRepository,
			BookRepository bookRepository,
			PasswordEncoder passwordEncoder
	) {
		return args -> {
			// 역할 초기화
			Role userRole = null;
			Role adminRole = null;
			
			if (roleRepository.findByName("USER").isEmpty()) {
				userRole = roleRepository.save(Role.builder().name("USER").build());
			} else {
				userRole = roleRepository.findByName("USER").get();
			}
			
			if (roleRepository.findByName("ADMIN").isEmpty()) {
				adminRole = roleRepository.save(Role.builder().name("ADMIN").build());
			} else {
				adminRole = roleRepository.findByName("ADMIN").get();
			}
			
			// 사용자 초기화 (책 소유자로 사용)
			User admin = null;
			User user = null;
			
			if (userRepository.findByEmail("admin@example.com").isEmpty()) {
				admin = User.builder()
						.firstname("Admin")
						.lastname("User")
						.email("admin@example.com")
						.password(passwordEncoder.encode("admin123"))
						.roles(List.of(adminRole))
						.build();
				admin = userRepository.save(admin);
			} else {
				admin = userRepository.findByEmail("admin@example.com").get();
			}
			
			if (userRepository.findByEmail("user@example.com").isEmpty()) {
				user = User.builder()
						.firstname("Normal")
						.lastname("User")
						.email("user@example.com")
						.password(passwordEncoder.encode("user123"))
						.roles(List.of(userRole))
						.build();
				user = userRepository.save(user);
			} else {
				user = userRepository.findByEmail("user@example.com").get();
			}
			
			// 책 데이터 초기화
			if (bookRepository.count() == 0) {
				List<Book> books = List.of(
					Book.builder()
						.title("클린 코드")
						.authorName("로버트 C. 마틴")
						.isbn("9788966260959")
						.synopsis("프로그래머가 반드시 알아야 할 깨끗한 코드를 작성하는 방법")
						.shareable(true)
						.archived(false)
						.createdBy(2)
						.build(),
						
					Book.builder()
						.title("스프링 부트와 AWS로 혼자 구현하는 웹 서비스")
						.authorName("이동욱")
						.isbn("9788965402602")
						.synopsis("스프링 부트와 AWS로 웹 애플리케이션을 구현하는 방법을 다룬 책")
						.shareable(true)
						.archived(false)
						.createdBy(2)
						.build(),
						
					Book.builder()
						.title("객체지향의 사실과 오해")
						.authorName("조영호")
						.isbn("9788998139766")
						.synopsis("객체지향에 대한 오해와 진실을 다룬 책")
						.shareable(true)
						.archived(false)
						.createdBy(2)
						.build(),
						
					Book.builder()
						.title("토비의 스프링")
						.authorName("이일민")
						.isbn("9788960773431")
						.synopsis("스프링 프레임워크의 원리와 이해")
						.shareable(false)
						.archived(false)
						.createdBy(2)
						.build(),
						
					Book.builder()
						.title("자바의 정석")
						.authorName("남궁성")
						.isbn("9788994492032")
						.synopsis("자바 프로그래밍 언어의 기초부터 고급 기능까지")
						.shareable(true)
						.archived(true)
						.createdBy(2)
						.build()
				);
				
				bookRepository.saveAll(books);
			}
		};
	}

}
