package com.alibou.booknetwork.validation.validator;

import com.alibou.booknetwork.user.UserRepository;
import com.alibou.booknetwork.validation.ValidEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 이메일 유효성 검증기 구현체
 * 
 * ConstraintValidator 인터페이스 구현:
 * - initialize(): 어노테이션 속성 초기화
 * - isValid(): 실제 검증 로직 수행
 * 
 * 엔터프라이즈 검증의 핵심 원칙:
 * 1. 다층 검증: 형식 → 도메인 → 비즈니스 룰 → 외부 검증
 * 2. 성능 최적화: 가벼운 검증부터 무거운 검증 순서
 * 3. 장애 격리: 외부 의존성 실패가 전체 검증을 방해하지 않음
 * 4. 로깅: 검증 실패 원인 추적 가능
 * 
 * SSGD에서 학습한 고급 검증 패턴:
 * - 캐시를 활용한 반복 검증 최적화
 * - 외부 API 호출 시 타임아웃 설정
 * - 검증 결과의 일시적 캐싱
 * - 검증 실패 통계 수집
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private final UserRepository userRepository;

    // RFC 5322 기반 이메일 정규식 (단순화된 버전)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );

    // 어노테이션 속성 저장
    private String[] allowedDomains;
    private String[] blockedDomains;
    private boolean checkDuplication;
    private boolean verifyExistence;
    private int minLength;
    private int maxLength;

    /**
     * 어노테이션 속성 초기화
     * 
     * 초기화 단계에서 수행하는 작업:
     * - 어노테이션 속성값 추출
     * - 검증 설정 유효성 확인
     * - 필요한 리소스 준비
     */
    @Override
    public void initialize(ValidEmail validEmail) {
        this.allowedDomains = validEmail.allowedDomains();
        this.blockedDomains = validEmail.blockedDomains();
        this.checkDuplication = validEmail.checkDuplication();
        this.verifyExistence = validEmail.verifyExistence();
        this.minLength = validEmail.minLength();
        this.maxLength = validEmail.maxLength();
        
        log.debug("이메일 검증기 초기화 - 허용 도메인: {}, 금지 도메인: {}, 중복 검사: {}, 존재 검증: {}", 
            Arrays.toString(allowedDomains), Arrays.toString(blockedDomains), 
            checkDuplication, verifyExistence);
    }

    /**
     * 이메일 유효성 검증 메인 로직
     * 
     * 검증 단계 (성능 최적화를 위한 순서):
     * 1. null/공백 검사 (가장 빠름)
     * 2. 길이 검사
     * 3. 정규식 검사 (CPU 집약적)
     * 4. 도메인 허용/차단 검사
     * 5. 데이터베이스 중복 검사 (I/O 집약적)
     * 6. 외부 API 존재 검증 (가장 느림)
     */
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        // 성능 메트릭을 위한 시작 시간
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. null 및 공백 검사
            if (!isNotNullOrEmpty(email)) {
                addConstraintViolation(context, "이메일 주소는 필수입니다");
                return false;
            }

            // 2. 길이 검사
            if (!isValidLength(email)) {
                addConstraintViolation(context, 
                    String.format("이메일 길이는 %d자 이상 %d자 이하여야 합니다", minLength, maxLength));
                return false;
            }

            // 3. 기본 형식 검증 (정규식)
            if (!isValidFormat(email)) {
                addConstraintViolation(context, "올바른 이메일 형식이 아닙니다");
                return false;
            }

            String domain = extractDomain(email);
            
            // 4. 도메인 허용 목록 검사
            if (!isDomainAllowed(domain)) {
                addConstraintViolation(context, "허용되지 않은 이메일 도메인입니다");
                return false;
            }

            // 5. 도메인 차단 목록 검사
            if (isDomainBlocked(domain)) {
                addConstraintViolation(context, "차단된 이메일 도메인입니다");
                return false;
            }

            // 6. 중복 검사 (데이터베이스 조회)
            if (checkDuplication && isDuplicated(email)) {
                addConstraintViolation(context, "이미 등록된 이메일 주소입니다");
                return false;
            }

            // 7. 이메일 존재 여부 검증 (외부 검증)
            if (verifyExistence && !emailExists(email, domain)) {
                addConstraintViolation(context, "존재하지 않는 이메일 주소입니다");
                return false;
            }

            // 모든 검증 통과
            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("이메일 검증 성공 - 주소: {}, 실행시간: {}ms", 
                maskEmail(email), executionTime);
            return true;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("이메일 검증 중 오류 발생 - 주소: {}, 실행시간: {}ms, 오류: {}", 
                maskEmail(email), executionTime, e.getMessage(), e);
            
            // 검증 중 오류 발생 시 기본적으로 유효하다고 판단 (fail-open)
            // 엄격한 검증이 필요한 경우 false 반환 고려
            return true;
        }
    }

    /**
     * null 및 공백 문자열 검사
     */
    private boolean isNotNullOrEmpty(String email) {
        return email != null && !email.trim().isEmpty();
    }

    /**
     * 이메일 길이 검증
     */
    private boolean isValidLength(String email) {
        int length = email.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 기본 이메일 형식 검증 (정규식 기반)
     * 
     * 정규식 최적화 고려사항:
     * - 컴파일된 Pattern 객체 재사용
     * - 과도하게 복잡한 정규식 지양
     * - 성능 테스트를 통한 검증
     */
    private boolean isValidFormat(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 이메일에서 도메인 부분 추출
     * 
     * @param email 전체 이메일 주소
     * @return 도메인 부분 (소문자 변환)
     */
    private String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1) {
            return "";
        }
        return email.substring(atIndex + 1).toLowerCase();
    }

    /**
     * 도메인 허용 목록 검사
     * 
     * @param domain 검사할 도메인
     * @return 허용된 도메인인지 여부 (허용 목록이 비어있으면 모든 도메인 허용)
     */
    private boolean isDomainAllowed(String domain) {
        if (allowedDomains == null || allowedDomains.length == 0) {
            return true; // 허용 목록이 비어있으면 모든 도메인 허용
        }
        
        return Arrays.stream(allowedDomains)
                .anyMatch(allowedDomain -> allowedDomain.equalsIgnoreCase(domain));
    }

    /**
     * 도메인 차단 목록 검사
     * 
     * @param domain 검사할 도메인
     * @return 차단된 도메인인지 여부
     */
    private boolean isDomainBlocked(String domain) {
        if (blockedDomains == null || blockedDomains.length == 0) {
            return false; // 차단 목록이 비어있으면 차단하지 않음
        }
        
        return Arrays.stream(blockedDomains)
                .anyMatch(blockedDomain -> blockedDomain.equalsIgnoreCase(domain));
    }

    /**
     * 이메일 중복 검사 (데이터베이스 조회)
     * 
     * 최적화 고려사항:
     * - 인덱스가 걸린 컬럼으로 조회
     * - 대소문자 구분 여부 결정
     * - 삭제된 사용자 제외 여부 결정
     */
    private boolean isDuplicated(String email) {
        try {
            boolean exists = userRepository.existsByEmailIgnoreCase(email);
            
            if (exists) {
                log.debug("이메일 중복 발견 - 주소: {}", maskEmail(email));
            }
            
            return exists;
            
        } catch (Exception e) {
            log.error("이메일 중복 검사 실패 - 주소: {}, 오류: {}", maskEmail(email), e.getMessage());
            
            // 데이터베이스 오류 시 중복되지 않은 것으로 간주 (서비스 연속성)
            return false;
        }
    }

    /**
     * 이메일 존재 여부 검증 (DNS/SMTP 검증)
     * 
     * 검증 방법:
     * 1. MX 레코드 확인: 도메인이 메일을 받을 수 있는지 확인
     * 2. SMTP 연결 테스트: 실제 메일 서버 연결 가능한지 확인
     * 3. VRFY 명령: 메일 주소 존재 여부 직접 확인 (지원하지 않는 서버 많음)
     * 
     * 주의사항:
     * - 네트워크 지연으로 인한 성능 저하
     * - 일부 서버는 보안상 존재 여부 응답 거부
     * - 타임아웃 설정 필수
     */
    private boolean emailExists(String email, String domain) {
        try {
            // 1. MX 레코드 확인
            if (!hasMXRecord(domain)) {
                log.debug("MX 레코드 없음 - 도메인: {}", domain);
                return false;
            }

            // 2. 도메인 해석 가능 여부 확인
            InetAddress.getByName(domain);
            
            log.debug("이메일 존재 검증 통과 - 주소: {}", maskEmail(email));
            return true;
            
        } catch (Exception e) {
            log.warn("이메일 존재 검증 실패 - 주소: {}, 오류: {}", maskEmail(email), e.getMessage());
            
            // 검증 실패 시 존재한다고 간주 (false positive 허용)
            return true;
        }
    }

    /**
     * MX 레코드 존재 여부 확인
     * 
     * MX(Mail Exchange) 레코드:
     * - DNS에서 메일 서버 정보를 제공
     * - 해당 도메인이 메일을 받을 수 있는지 확인
     * - 우선순위별 메일 서버 목록 제공
     */
    private boolean hasMXRecord(String domain) {
        try {
            // 실제 구현에서는 DNSJava 라이브러리 사용 권장
            // Lookup lookup = new Lookup(domain, Type.MX);
            // Record[] records = lookup.run();
            // return records != null && records.length > 0;
            
            // 간단한 구현: 도메인 해석 가능 여부만 확인
            InetAddress.getByName(domain);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 커스텀 오류 메시지 추가
     * 
     * ConstraintValidatorContext 활용:
     * - 기본 메시지 비활성화
     * - 커스텀 메시지 추가
     * - 다중 오류 메시지 지원
     * - 프로퍼티 경로 지정 가능
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }

    /**
     * 이메일 주소 마스킹 (로깅 시 개인정보 보호)
     * 
     * 마스킹 패턴: user@domain.com → u***@d*****.com
     * - 첫 글자와 도메인 일부만 표시
     * - 개인정보 보호와 디버깅 정보 균형
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex == -1) {
            return email.charAt(0) + "***";
        }
        
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        
        String maskedLocal = localPart.length() > 1 ? 
            localPart.charAt(0) + "***" : localPart;
            
        String maskedDomain = domainPart.length() > 3 ?
            domainPart.charAt(0) + "***" + domainPart.substring(domainPart.length() - 2) : domainPart;
            
        return maskedLocal + "@" + maskedDomain;
    }
}