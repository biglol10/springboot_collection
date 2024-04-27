package com.fastcampus.projectboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@EnableJpaAuditing // Entity에서도 auditing을 쓴다는 표시를 해줘야 함 (@EntityListeners(AuditingEntityListener.class))
@Configuration // Configuration Bean이 되게끔. 각종 설정을 정의할 때
public class JpaConfig {
    @Bean
    public AuditorAware<String> auditorAware() { // id가 들어감. auditing을 할 때마다 여기에 해당되는 id가 들어감
        return () -> Optional.of("biglol"); // TODO: 스프링 시큐리티로 인증 기능을 붙이게 될 때 수정 필요
    }
}

//영속성 컨텍스트(Persistence Context)는 JPA에서 엔티티(Entity)들을 저장하고 관리하는 환경을 말합니다. 이 개념은 엔티티의 생명주기를 관리하고, 데이터베이스와의 일관된 데이터 교환을 보장하기 위해 도입되었습니다. 영속성 컨텍스트 내에서 엔티티의 상태에 따라 다음과 같은 생명주기 상태를 갖습니다:
//
//비영속 (New/Transient): 엔티티가 생성되었지만 아직 영속성 컨텍스트와 관련이 없는 상태입니다. 이 상태의 엔티티는 데이터베이스에 저장되지 않았으며, JPA가 관리하지 않습니다.
//
//영속 (Managed/Persistent): 엔티티가 영속성 컨텍스트에 저장된 상태입니다. 이 상태의 엔티티는 JPA가 관리하며, 트랜잭션이 커밋될 때 데이터베이스에 반영됩니다.
//
//준영속 (Detached): 영속 상태였던 엔티티가 영속성 컨텍스트에서 분리된 상태입니다. 이 상태의 엔티티는 더 이상 JPA가 관리하지 않으며, 데이터베이스와의 동기화도 이루어지지 않습니다.
//
//삭제 (Removed): 엔티티가 영속성 컨텍스트에서 삭제되어 데이터베이스에서도 삭제될 예정인 상태입니다.
//
//영속성 컨텍스트의 주요 이점은 다음과 같습니다:
//
//        1차 캐시: 영속성 컨텍스트는 조회된 엔티티를 내부 캐시에 저장하여, 같은 트랜잭션 내에서 동일한 엔티티에 대한 반복된 조회 요청이 있을 경우 데이터베이스 접근 없이 빠르게 데이터를 제공합니다.
//        동일성 보장 (Identity Guarantee): 영속성 컨텍스트는 같은 엔티티에 대해 항상 같은 인스턴스를 반환함으로써 애플리케이션에서 엔티티의 동일성을 보장합니다.
//        변경 감지 (Dirty Checking): 영속 상태의 엔티티에 대한 변경 사항을 자동으로 감지하고, 트랜잭션이 커밋될 때 이러한 변경 사항을 데이터베이스에 자동으로 반영합니다.
//지연 로딩 (Lazy Loading): 엔티티의 연관된 데이터를 실제로 사용될 때까지 로딩을 지연시켜, 불필요한 데이터베이스 접근을 줄이고 성능을 향상시킵니다.
//영속성 컨텍스트는 JPA를 사용하는 애플리케이션에서 데이터 접근 계층의 성능 최적화와 일관성 유지에 중요한 역할을 합니다.