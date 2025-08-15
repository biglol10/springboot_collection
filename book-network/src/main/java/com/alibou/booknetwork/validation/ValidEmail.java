package com.alibou.booknetwork.validation;

import com.alibou.booknetwork.validation.validator.EmailValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 이메일 유효성 검증 커스텀 어노테이션
 * 
 * 커스텀 Validation의 엔터프라이즈 가치:
 * 1. 비즈니스 룰 중앙화: 검증 로직을 한 곳에서 관리
 * 2. 선언적 검증: 어노테이션 기반의 직관적인 검증 설정
 * 3. 재사용성: 여러 클래스에서 동일한 검증 로직 사용
 * 4. 유지보수성: 검증 룰 변경 시 한 곳만 수정
 * 
 * SSGD에서 학습한 검증 패턴:
 * - 도메인별 세분화된 검증 룰
 * - 외부 시스템과의 검증 연동 (이메일 도메인 화이트리스트)
 * - 다국어 오류 메시지 지원
 * - 실시간 검증 피드백
 * 
 * Spring Validation Framework 활용:
 * - JSR-303/JSR-380 Bean Validation 표준 준수
 * - @Valid, @Validated와 완벽 연동
 * - MethodValidation을 통한 메서드 파라미터 검증
 * - 그룹 검증을 통한 조건부 검증
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailValidator.class)
@Documented
public @interface ValidEmail {
    
    /**
     * 기본 오류 메시지
     * 
     * 메시지 국제화:
     * - messages.properties 파일에서 키 기반 메시지 관리
     * - 다국어 지원을 위한 메시지 키 사용
     * - 동적 메시지 생성 가능
     */
    String message() default "올바른 이메일 형식이 아닙니다";
    
    /**
     * 검증 그룹
     * 
     * 그룹 검증의 활용:
     * - 회원가입 vs 로그인 시 다른 검증 룰 적용
     * - 관리자 vs 일반 사용자 검증 차별화
     * - 단계별 검증 (기본 검증 → 고급 검증)
     */
    Class<?>[] groups() default {};
    
    /**
     * 페이로드
     * 
     * 페이로드 활용:
     * - 검증 실패 시 추가 메타데이터 전달
     * - 오류 심각도 레벨 설정
     * - 커스텀 오류 처리 로직 트리거
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 허용할 이메일 도메인 목록 (빈 배열이면 모든 도메인 허용)
     * 
     * 도메인 제한 시나리오:
     * - 기업 내부 시스템: 회사 도메인만 허용
     * - B2B 시스템: 파트너 기업 도메인만 허용
     * - 보안이 중요한 서비스: 무료 이메일 서비스 제외
     * 
     * 예시: {"company.com", "partner.com"}
     */
    String[] allowedDomains() default {};
    
    /**
     * 금지할 이메일 도메인 목록 (스팸, 임시 이메일 서비스 등)
     * 
     * 금지 도메인 관리:
     * - 임시 이메일 서비스 차단
     * - 스팸으로 알려진 도메인 차단
     * - 보안상 위험한 도메인 차단
     * 
     * 예시: {"tempmail.com", "10minutemail.com", "guerrillamail.com"}
     */
    String[] blockedDomains() default {};
    
    /**
     * 이메일 중복 검사 여부
     * 
     * 중복 검사 시나리오:
     * - 회원가입: 중복 검사 필요
     * - 프로필 수정: 본인 이메일은 허용, 다른 사람 이메일은 금지
     * - 관리자 기능: 중복 검사 생략 가능
     */
    boolean checkDuplication() default false;
    
    /**
     * 이메일 존재 여부 검증 (실제 존재하는 이메일인지 확인)
     * 
     * 주의사항:
     * - 네트워크 호출로 인한 성능 영향
     * - 외부 서비스 의존성 증가
     * - 일부 메일 서버는 존재 여부 확인 차단
     * - 개발환경에서는 비활성화 권장
     */
    boolean verifyExistence() default false;
    
    /**
     * 최소 이메일 길이
     */
    int minLength() default 5;
    
    /**
     * 최대 이메일 길이 (RFC 5321에 따르면 최대 320자)
     */
    int maxLength() default 320;
}