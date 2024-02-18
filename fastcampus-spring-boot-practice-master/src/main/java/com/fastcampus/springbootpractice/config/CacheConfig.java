package com.fastcampus.springbootpractice.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

// 등장하게 됨 배경:
// 이런 설정을 해주지 않고 바로 redis를 적용하면 유니코드 문자로 보임
// Redis가 되면 외부 저장소랑 통신을 해야 되니까 우리가 캐싱하고 싶은 자료를 직렬화해서 보내줘야 함
// 그리고 돌아올 땐 역직렬화
// 그래서 그냥 spring.cache.type=redis 만 하면 사실 동작 안함
// public class Student implements Serializable을 해줘야 함. 이게 빠지면 에러발생
// 그런데 이건 자바코드를 건드리는거고 json으로 바꾸기 위해 CacheConfig 작성

@EnableCaching
@Configuration
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(name -> name + ":") // 안해주면 student::fred로 되니 student:fred로 변경
                .entryTtl(Duration.ofSeconds(10))  // jacksonSerializer쓰면 spring.cache.redids.time-to-live가 동작하지 않기 때문에 여기에서 설정
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));  // 직렬화 도구를 바꿈. implements Serializable 빼면 됨
    }

}
