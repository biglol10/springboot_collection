package com.alibou.booknetwork.validation;

import com.alibou.booknetwork.validation.validator.ISBNValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * ISBN(International Standard Book Number) 유효성 검증 어노테이션
 * 
 * ISBN 표준의 중요성:
 * 1. 국제 표준: 전 세계적으로 통용되는 도서 식별 체계
 * 2. 유일성: 각 도서판마다 고유한 식별자 보장
 * 3. 체크섬: 입력 오류 방지를 위한 검증 숫자 포함
 * 4. 유통 관리: 출판사, 서점, 도서관에서 재고 및 주문 관리 용이
 * 
 * ISBN-10 vs ISBN-13:
 * - ISBN-10: 2007년 이전 표준 (10자리)
 * - ISBN-13: 2007년 이후 표준 (13자리, EAN-13 기반)
 * - 현재는 ISBN-13이 표준이지만 ISBN-10도 여전히 사용
 * 
 * 도서 관리 시스템에서의 활용:
 * - 도서 등록 시 중복 방지
 * - 외부 API 연동 시 표준 식별자 활용
 * - 바코드 스캔을 통한 자동 도서 인식
 * - 도서관 시스템과의 호환성 확보
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ISBNValidator.class)
@Documented
public @interface ValidISBN {
    
    /**
     * 기본 오류 메시지
     */
    String message() default "올바른 ISBN 형식이 아닙니다";
    
    /**
     * 검증 그룹
     */
    Class<?>[] groups() default {};
    
    /**
     * 페이로드
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * ISBN-10 형식 허용 여부
     * 
     * true: ISBN-10과 ISBN-13 모두 허용
     * false: ISBN-13만 허용
     * 
     * 비즈니스 요구사항에 따라 설정:
     * - 신간 위주: ISBN-13만 허용
     * - 기존 도서 포함: ISBN-10도 허용
     */
    boolean allowIsbn10() default true;
    
    /**
     * 하이픈(-) 구분자 허용 여부
     * 
     * true: "978-89-123-4567-8" 형태 허용
     * false: "9788912345678" 형태만 허용
     * 
     * 사용자 편의성과 정규화 요구사항 고려
     */
    boolean allowHyphens() default true;
    
    /**
     * 공백 문자 허용 여부
     * 
     * 사용자 입력 시 실수로 입력된 공백 처리
     */
    boolean allowSpaces() default false;
    
    /**
     * ISBN 중복 검사 여부
     * 
     * 도서 등록 시 동일한 ISBN의 도서가 이미 존재하는지 확인
     * 주의: 동일 도서의 다른 버전(판본, 형태)은 다른 ISBN 사용
     */
    boolean checkDuplication() default false;
    
    /**
     * 외부 ISBN 데이터베이스 검증 여부
     * 
     * 실제 존재하는 ISBN인지 외부 서비스로 확인
     * 예: Google Books API, Open Library API
     * 
     * 주의사항:
     * - 네트워크 지연 및 장애 가능성
     * - 외부 서비스 사용량 제한
     * - 개발환경에서는 비활성화 권장
     */
    boolean verifyWithExternalService() default false;
}