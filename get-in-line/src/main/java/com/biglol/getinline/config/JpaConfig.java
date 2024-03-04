package com.biglol.getinline.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@EnableJpaAuditing
@Configuration
public class JpaConfig { // 오로지 jpa auditing기능 추가하고 싶어서 넣은거임 (JPA Auditing은 엔티티의 생성, 수정 시간 등을 자동으로 관리해주는 기능입니다)
//
//    // application.properties에서 jpa관련 설정을 다 끝냈기에 아래 소스는 필요없지만 필요에 따라 직접 세팅이 필요할 때. 아래 코드는 application.properties에서 설정한 것과 같음
////    @ConfigurationProperties("spring.datasource") (아래 *1 내용 참고)
//    @Bean
//    public DataSource dataSource() {
//        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
//        return builder.setType(EmbeddedDatabaseType.H2).build();
//
////        // 만약에 h2가 아닌 직접 jdbc쪽으로 쓰고 싶으면
////        return DataSourceBuilder.create().driverClassName().type().url().username().password().build();
//
////        // (*1) 아니면 이렇게 간단하게 하고 @ConfigurationProperties("spring.datasource")를 설정하여 application.properties에 설정한 property들이 주입되게끔 함
////        return DataSourceBuilder.create().build();
//    }
//
//    // 만약에 데이터소스가 2개이면 같은 내용으로 등록
//    // 예시: dataSource2, entityManagerFactory2, transactionManager2
//    // 그럼 Transactional만 가지고 단순하게 우리가 원하는 데이터 소스, 즉 우리가 원하는 db에 접근할 수 없음
//    // 동일한 서비스 로직에서 2개의 db에 접근을 하고 싶을 때 에러 사항이 생김
//    // 그래서 Transaction manager을 하나로 묶어줘야 하는데 기존엔 *2와 같은 방법을 썼는데 deprecated됨.
//    // chatgpt에 물어본 결과는 아래 전체주석으로 되어있음
//
//
//    @Bean
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory() { // 또는 (DataSource dataSource)도 가능 dataSource가 무사히 Bean으로 등록되었을 경우
//        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
//        vendorAdapter.setGenerateDdl(true);
//
//        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
//        factory.setJpaVendorAdapter(vendorAdapter);
//        factory.setPackagesToScan("com.acme.domain");
//        factory.setDataSource(dataSource()); // 메소드 호출 또는 dataSource 빈을 직접 사용
//        return factory;
//    }
//
//    @Bean
//    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
//        JpaTransactionManager txManager = new JpaTransactionManager();
//        txManager.setEntityManagerFactory(entityManagerFactory);
//        return txManager;
//    }
//
//    // (*2)
////    @Bean
////    public ChainedTransactionManager chainedTransactionManager(PlatformTransactionManager transactionManager) { // 여러개 넣기 가능. 중요하고 실패하는 것에 대해 좀더 민감한 트랜잭션을 뒤에 배치해야 함
////        return new ChainedTransactionManager(transactionManager);
////    }

}



//@Configuration
//@EnableTransactionManagement
//public class DataSourceConfig {
//
//    @Primary
//    @Bean
//    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
//            EntityManagerFactoryBuilder builder, @Qualifier("dataSource") DataSource dataSource) {
//        return builder
//                .dataSource(dataSource)
//                .packages("com.example.domain.primary")
//                .persistenceUnit("primary")
//                .build();
//    }
//
//    @Bean
//    public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory(
//            EntityManagerFactoryBuilder builder, @Qualifier("secondaryDataSource") DataSource dataSource) {
//        return builder
//                .dataSource(dataSource)
//                .packages("com.example.domain.secondary")
//                .persistenceUnit("secondary")
//                .build();
//    }
//
//    @Primary
//    @Bean
//    public PlatformTransactionManager primaryTransactionManager(
//            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
//        return new JpaTransactionManager(entityManagerFactory);
//    }
//
//    @Bean
//    public PlatformTransactionManager secondaryTransactionManager(
//            @Qualifier("secondaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
//        return new JpaTransactionManager(entityManagerFactory);
//    }
//}


//@Service
//public class SomeService {
//
//    @Transactional(transactionManager = "primaryTransactionManager")
//    public void somePrimaryServiceMethod() {
//        // primary 데이터 소스 작업
//    }
//
//    @Transactional(transactionManager = "secondaryTransactionManager")
//    public void someSecondaryServiceMethod() {
//        // secondary 데이터 소스 작업
//    }
//}
