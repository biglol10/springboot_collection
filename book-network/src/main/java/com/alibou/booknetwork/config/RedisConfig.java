package com.alibou.booknetwork.config;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {
    /**
     * Redis 템플릿 빈 설정
     * 
     * @param connectionFactory Redis 연결 팩토리 (Spring Boot가 자동으로 주입)
     * @return 구성된 RedisTemplate 객체
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // RedisTemplate 인스턴스 생성 - Redis 작업을 위한 고수준 추상화 제공
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        // Redis 연결 팩토리 설정 - Redis 서버와의 연결 관리
        template.setConnectionFactory(connectionFactory);
        
        // 키 직렬화 도구 설정 - Redis에 저장될 키를 문자열로 직렬화
        template.setKeySerializer(new StringRedisSerializer());

        // 값 직렬화 도구 설정 - Redis에 저장될 값을 JSON 형식으로 직렬화
        // GenericJackson2JsonRedisSerializer는 객체를 JSON으로 변환하여 Redis에 저장합니다.
        // 이 직렬화기는 객체의 클래스 정보를 JSON에 포함시켜 역직렬화 시 원본 타입으로 복원할 수 있게 합니다.
        // BookResponse, PageResponse 등의 객체가 Redis에 저장될 때 이 직렬화기를 통해 처리됩니다.
        // 단, 이 방식은 JSON에 타입 정보가 포함되어 저장 공간을 더 사용하지만, 다양한 객체 타입을 안전하게 처리할 수 있습니다.
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        /**
         * 다르게 하면
         * 
         * [
    "com.alibou.booknetwork.common.PageResponse",
    {
        "content": [
            "java.util.ImmutableCollections$ListN",
            [
                [
                    "com.alibou.booknetwork.book.BookResponse",
                    {
                        "id": 55,
                        "title": "토비의 스프링",
                        "authorName": "이일민",
                        "isbn": "9788960773431",
                        "synopsis": "스프링 프레임워크의 원리와 이해",
                        "owner": null,
                        "cover": null,
                        "rate": 0.0,
                        "archived": false,
                        "shareable": false
                    }
                ]
            ]
        ]
            처럼 떠서 그냥 GenericJackson2JsonRedisSerializer 씀
         */

         /**
          * 키값은
          1) "books::title-\xed\x86\xa0\xeb\xb9\x84:authorName-null:isbn-null:synopsis-null:bookCover-null:archived-null:shareable-null"
2) "books::title-null:authorName-null:isbn-null:synopsis-null:bookCover-null:archived-null:shareable-null"
처럼 저장됨
          */
        
        // 구성된 RedisTemplate 반환 - 이제 이 템플릿으로 Redis 작업 수행 가능
        return template;
    }

    /**
     * Redis 직렬화에 사용할 ObjectMapper 빈 설정
     * 
     * @return 구성된 ObjectMapper 객체
     */
    // @Bean(name = "cacheObjectMapper")
    // public ObjectMapper objectMapper() {
    //     // ObjectMapper 인스턴스 생성 - JSON 직렬화/역직렬화를 담당
    //     ObjectMapper objectMapper = new ObjectMapper();
        
    //     // JavaTimeModule 등록 - Java 8 날짜/시간 타입(LocalDate, LocalDateTime 등)을 처리
    //     objectMapper.registerModule(new JavaTimeModule());
        
    //     // 다형성 타입 처리 활성화 - 객체의 클래스 정보를 JSON에 포함시켜 역직렬화 시 원본 타입으로 복원 가능
    //     // BasicPolymorphicTypeValidator로 직렬화 허용 타입 제한 - 보안 강화
    //     objectMapper.activateDefaultTyping(
    //             BasicPolymorphicTypeValidator.builder()
    //                     .allowIfBaseType(Object.class)  // Object 클래스를 기본 타입으로 허용
    //                     .allowIfSubType(Object.class)   // Object의 모든 하위 타입 허용
    //                     .build(),
    //             ObjectMapper.DefaultTyping.NON_FINAL);  // final이 아닌 클래스에 대해 타입 정보 포함
        
    //     // 날짜를 타임스탬프가 아닌 ISO-8601 형식으로 직렬화하도록 설정
    //     // 예: "2023-01-01T12:00:00" 형식으로 저장됨
    //     objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
    //     return objectMapper;
    // }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 기본 Redis 캐시 설정 구성 객체 생성
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            // 캐시 항목의 기본 유효 시간(TTL)을 10분으로 설정
            .entryTtl(Duration.ofMinutes(10))
            // 캐시 키를 직렬화하는 방법 설정 - 문자열 형식으로 직렬화
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            // 캐시 값을 직렬화하는 방법 설정 - JSON 형식으로 직렬화하여 객체 저장 가능
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            // 참고: 이 설정은 RedisTemplate의 setValueSerializer와 별개로 작동합니다.
            // RedisTemplate은 일반 Redis 작업에 사용되고, 
            // RedisCacheManager는 Spring의 캐시 추상화에 사용됩니다.
            // Redis는 인메모리 데이터 저장소로 빠른 데이터 접근을 제공하는 NoSQL 데이터베이스입니다.
            // Spring의 캐시 추상화는 @Cacheable, @CacheEvict 등의 어노테이션을 통해 
            // 메서드 결과를 캐싱하는 표준화된 방법을 제공합니다.
            // RedisTemplate은 Redis와 직접 상호작용하는 저수준 작업에 사용되고,
            // RedisCacheManager는 Spring의 캐시 추상화 API를 통해 Redis를 캐시 저장소로 사용합니다.
            // null 값은 캐싱하지 않도록 설정 - 불필요한 메모리 사용 방지
            .disableCachingNullValues();

        // Redis 캐시 매니저 빌더를 사용하여 캐시 매니저 생성
        return RedisCacheManager.builder(connectionFactory)
            // 위에서 정의한 기본 캐시 설정 적용
            .cacheDefaults(cacheConfig)
            // "books" 캐시에 대한 특별 설정 - 유효 시간을 5분으로 설정
            .withCacheConfiguration("books", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)))
            // "users" 캐시에 대한 특별 설정 - 유효 시간을 1분으로 설정 (더 자주 갱신 필요)
            .withCacheConfiguration("users", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1)))
            // 설정이 완료된 캐시 매니저 빌드하여 반환
            .build();
    }
}
