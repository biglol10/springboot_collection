package com.alibou.booknetwork.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 분산 추적 어노테이션
 * 
 * 이 어노테이션을 메서드에 적용하면 해당 메서드의 실행이 자동으로 추적됩니다.
 * 
 * 사용 예시:
 * ```java
 * @Traced(
 *     value = "book.registration",
 *     domain = "library",
 *     operation = "create",
 *     performanceThresholdMs = 1000
 * )
 * public Book registerBook(BookRequest request) {
 *     // 비즈니스 로직
 * }
 * ```
 * 
 * 주요 기능:
 * - 자동 스팬 생성 및 관리
 * - 비즈니스 도메인별 태그 추가
 * - 성능 임계값 모니터링
 * - 에러 자동 추적
 * - 파라미터 및 반환값 로깅 제어
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {

    /**
     * 스팬 이름
     * 기본값: 클래스명.메서드명
     */
    String value() default "";

    /**
     * 비즈니스 도메인
     * 예: "library", "user-management", "notification"
     */
    String domain() default "";

    /**
     * 비즈니스 연산 타입
     * 예: "create", "update", "delete", "search", "recommend"
     */
    String operation() default "";

    /**
     * 메서드 파라미터를 스팬 태그에 포함할지 여부
     * 주의: 민감한 정보가 포함된 경우 false로 설정
     */
    boolean includeParameters() default false;

    /**
     * 메서드 반환값을 스팬 태그에 포함할지 여부
     * 주의: 민감한 정보가 포함된 경우 false로 설정
     */
    boolean includeResult() default false;

    /**
     * 예외 발생 시 스택 트레이스를 포함할지 여부
     */
    boolean includeStackTrace() default true;

    /**
     * 성능 임계값 (밀리초)
     * 이 값보다 오래 걸리면 성능 경고 태그가 추가됩니다.
     * 0이면 임계값 체크하지 않음
     */
    long performanceThresholdMs() default 0;

    /**
     * 비동기 실행 여부
     * true이면 새로운 트레이스 컨텍스트에서 실행
     */
    boolean async() default false;

    /**
     * 추가 커스텀 태그들
     * "key1=value1,key2=value2" 형식
     */
    String[] tags() default {};
}