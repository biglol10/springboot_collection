package com.alibou.booknetwork.validation.validator;

import com.alibou.booknetwork.book.BookRepository;
import com.alibou.booknetwork.validation.ValidISBN;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ISBN 유효성 검증기 구현체
 * 
 * ISBN 검증의 핵심 알고리즘:
 * 1. 형식 정규화: 하이픈, 공백 제거
 * 2. 길이 검사: ISBN-10(10자리) 또는 ISBN-13(13자리)
 * 3. 숫자 검사: 마지막 자리 제외하고 모두 숫자
 * 4. 체크섬 검증: 각 ISBN 표준의 체크섬 알고리즘 적용
 * 5. 비즈니스 검증: 중복 검사, 외부 서비스 검증
 * 
 * 체크섬 알고리즘의 중요성:
 * - 입력 오류 95% 이상 감지 가능
 * - 자릿수 뒤바뀜 오류 감지
 * - 인접한 숫자 교체 오류 감지
 * - 바코드 스캔 오류 방지
 * 
 * 엔터프라이즈 검증 설계 원칙:
 * - 빠른 실패: 가벼운 검증부터 수행
 * - 정규화 우선: 사용자 입력의 다양한 형태 수용
 * - 캐싱 활용: 외부 검증 결과 임시 저장
 * - 장애 허용: 외부 서비스 실패 시 서비스 지속
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ISBNValidator implements ConstraintValidator<ValidISBN, String> {

    private final BookRepository bookRepository;

    // 어노테이션 속성
    private boolean allowIsbn10;
    private boolean allowHyphens;
    private boolean allowSpaces;
    private boolean checkDuplication;
    private boolean verifyWithExternalService;

    @Override
    public void initialize(ValidISBN validISBN) {
        this.allowIsbn10 = validISBN.allowIsbn10();
        this.allowHyphens = validISBN.allowHyphens();
        this.allowSpaces = validISBN.allowSpaces();
        this.checkDuplication = validISBN.checkDuplication();
        this.verifyWithExternalService = validISBN.verifyWithExternalService();
        
        log.debug("ISBN 검증기 초기화 - ISBN-10 허용: {}, 하이픈 허용: {}, 공백 허용: {}, 중복 검사: {}, 외부 검증: {}", 
            allowIsbn10, allowHyphens, allowSpaces, checkDuplication, verifyWithExternalService);
    }

    @Override
    public boolean isValid(String isbn, ConstraintValidatorContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. null 및 공백 검사
            if (isbn == null || isbn.trim().isEmpty()) {
                addConstraintViolation(context, "ISBN은 필수입니다");
                return false;
            }

            // 2. ISBN 정규화 (하이픈, 공백 처리)
            String normalizedIsbn = normalizeIsbn(isbn);
            if (normalizedIsbn == null) {
                addConstraintViolation(context, "ISBN 형식이 올바르지 않습니다");
                return false;
            }

            // 3. 길이 및 기본 형식 검사
            if (!isValidFormat(normalizedIsbn)) {
                addConstraintViolation(context, "ISBN 길이가 올바르지 않습니다");
                return false;
            }

            // 4. 체크섬 검증
            if (!isValidChecksum(normalizedIsbn)) {
                addConstraintViolation(context, "ISBN 체크섬이 올바르지 않습니다");
                return false;
            }

            // 5. 중복 검사
            if (checkDuplication && isDuplicated(normalizedIsbn)) {
                addConstraintViolation(context, "이미 등록된 ISBN입니다");
                return false;
            }

            // 6. 외부 서비스 검증
            if (verifyWithExternalService && !verifyWithExternalAPI(normalizedIsbn)) {
                addConstraintViolation(context, "존재하지 않는 ISBN입니다");
                return false;
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("ISBN 검증 성공 - ISBN: {}, 실행시간: {}ms", 
                maskIsbn(normalizedIsbn), executionTime);
            return true;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("ISBN 검증 중 오류 발생 - ISBN: {}, 실행시간: {}ms, 오류: {}", 
                maskIsbn(isbn), executionTime, e.getMessage(), e);
            return true; // fail-open
        }
    }

    /**
     * ISBN 정규화
     * 
     * 정규화 과정:
     * 1. 대소문자 통일 (ISBN-10의 마지막 자리 'X' 처리)
     * 2. 하이픈(-) 제거 (설정에 따라)
     * 3. 공백 제거 (설정에 따라)
     * 4. 유효하지 않은 문자 검사
     * 
     * @param isbn 원본 ISBN
     * @return 정규화된 ISBN 또는 null (유효하지 않은 경우)
     */
    private String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }

        String normalized = isbn.trim().toUpperCase();

        // 하이픈 처리
        if (!allowHyphens && normalized.contains("-")) {
            log.debug("하이픈이 허용되지 않는 ISBN - 입력: {}", isbn);
            return null;
        }
        normalized = normalized.replace("-", "");

        // 공백 처리
        if (!allowSpaces && normalized.contains(" ")) {
            log.debug("공백이 허용되지 않는 ISBN - 입력: {}", isbn);
            return null;
        }
        normalized = normalized.replace(" ", "");

        // 유효한 문자만 포함하는지 검사 (숫자와 X만 허용)
        if (!normalized.matches("^[0-9X]+$")) {
            log.debug("유효하지 않은 문자 포함 - ISBN: {}", isbn);
            return null;
        }

        return normalized;
    }

    /**
     * 기본 형식 검증
     * 
     * 검증 항목:
     * - 길이: ISBN-10(10자리) 또는 ISBN-13(13자리)
     * - ISBN-10: 처음 9자리는 숫자, 마지막은 숫자 또는 'X'
     * - ISBN-13: 978 또는 979로 시작, 모든 자리 숫자
     */
    private boolean isValidFormat(String isbn) {
        if (isbn.length() == 10) {
            // ISBN-10 형식 검사
            if (!allowIsbn10) {
                log.debug("ISBN-10이 허용되지 않음 - ISBN: {}", isbn);
                return false;
            }
            
            // 처음 9자리는 숫자, 마지막은 숫자 또는 X
            return isbn.substring(0, 9).matches("^[0-9]{9}$") && 
                   isbn.charAt(9) >= '0' && isbn.charAt(9) <= '9' || isbn.charAt(9) == 'X';
                   
        } else if (isbn.length() == 13) {
            // ISBN-13 형식 검사
            // 978 또는 979로 시작해야 함
            if (!isbn.startsWith("978") && !isbn.startsWith("979")) {
                log.debug("ISBN-13이 978 또는 979로 시작하지 않음 - ISBN: {}", isbn);
                return false;
            }
            
            // 모든 자리가 숫자여야 함
            return isbn.matches("^[0-9]{13}$");
            
        } else {
            log.debug("유효하지 않은 ISBN 길이 - 길이: {}, ISBN: {}", isbn.length(), isbn);
            return false;
        }
    }

    /**
     * 체크섬 검증
     * 
     * ISBN-10 체크섬 알고리즘:
     * - 각 자리수에 가중치(10,9,8,...,2)를 곱하여 합산
     * - 합을 11로 나눈 나머지를 11에서 뺀 값이 체크 디지트
     * - 결과가 10이면 'X', 11이면 0
     * 
     * ISBN-13 체크섬 알고리즘:
     * - 홀수 자리는 1배, 짝수 자리는 3배하여 합산
     * - 합을 10으로 나눈 나머지를 10에서 뺀 값이 체크 디지트
     * - 결과가 10이면 0
     */
    private boolean isValidChecksum(String isbn) {
        if (isbn.length() == 10) {
            return isValidIsbn10Checksum(isbn);
        } else if (isbn.length() == 13) {
            return isValidIsbn13Checksum(isbn);
        }
        return false;
    }

    /**
     * ISBN-10 체크섬 검증
     */
    private boolean isValidIsbn10Checksum(String isbn) {
        try {
            int sum = 0;
            
            // 처음 9자리 계산
            for (int i = 0; i < 9; i++) {
                int digit = Character.getNumericValue(isbn.charAt(i));
                sum += digit * (10 - i);
            }
            
            // 체크 디지트 계산
            int remainder = sum % 11;
            int checkDigit = (11 - remainder) % 11;
            
            // 마지막 자리와 비교
            char lastChar = isbn.charAt(9);
            if (checkDigit == 10) {
                return lastChar == 'X';
            } else {
                return Character.getNumericValue(lastChar) == checkDigit;
            }
            
        } catch (Exception e) {
            log.warn("ISBN-10 체크섬 계산 오류 - ISBN: {}, 오류: {}", isbn, e.getMessage());
            return false;
        }
    }

    /**
     * ISBN-13 체크섬 검증
     */
    private boolean isValidIsbn13Checksum(String isbn) {
        try {
            int sum = 0;
            
            // 처음 12자리 계산
            for (int i = 0; i < 12; i++) {
                int digit = Character.getNumericValue(isbn.charAt(i));
                int weight = (i % 2 == 0) ? 1 : 3; // 홀수 자리는 1배, 짝수 자리는 3배
                sum += digit * weight;
            }
            
            // 체크 디지트 계산
            int remainder = sum % 10;
            int checkDigit = (10 - remainder) % 10;
            
            // 마지막 자리와 비교
            int lastDigit = Character.getNumericValue(isbn.charAt(12));
            return lastDigit == checkDigit;
            
        } catch (Exception e) {
            log.warn("ISBN-13 체크섬 계산 오류 - ISBN: {}, 오류: {}", isbn, e.getMessage());
            return false;
        }
    }

    /**
     * ISBN 중복 검사
     */
    private boolean isDuplicated(String isbn) {
        try {
            // ISBN-10과 ISBN-13 상호 변환을 고려한 중복 검사
            boolean exists = bookRepository.existsByIsbn(isbn);
            
            if (!exists && isbn.length() == 13 && allowIsbn10) {
                // ISBN-13을 ISBN-10으로 변환하여 중복 검사
                String isbn10 = convertIsbn13ToIsbn10(isbn);
                if (isbn10 != null) {
                    exists = bookRepository.existsByIsbn(isbn10);
                }
            } else if (!exists && isbn.length() == 10) {
                // ISBN-10을 ISBN-13으로 변환하여 중복 검사
                String isbn13 = convertIsbn10ToIsbn13(isbn);
                if (isbn13 != null) {
                    exists = bookRepository.existsByIsbn(isbn13);
                }
            }
            
            if (exists) {
                log.debug("ISBN 중복 발견 - ISBN: {}", maskIsbn(isbn));
            }
            
            return exists;
            
        } catch (Exception e) {
            log.error("ISBN 중복 검사 실패 - ISBN: {}, 오류: {}", maskIsbn(isbn), e.getMessage());
            return false; // 오류 시 중복되지 않은 것으로 간주
        }
    }

    /**
     * ISBN-13을 ISBN-10으로 변환
     * 
     * 변환 조건: 978로 시작하는 ISBN-13만 변환 가능
     * (979로 시작하는 ISBN-13은 ISBN-10으로 변환 불가)
     */
    private String convertIsbn13ToIsbn10(String isbn13) {
        if (!isbn13.startsWith("978")) {
            return null; // 979로 시작하는 경우 변환 불가
        }
        
        try {
            // 978 제거 후 9자리 추출
            String isbn10WithoutCheck = isbn13.substring(3, 12);
            
            // ISBN-10 체크 디지트 계산
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                int digit = Character.getNumericValue(isbn10WithoutCheck.charAt(i));
                sum += digit * (10 - i);
            }
            
            int remainder = sum % 11;
            int checkDigit = (11 - remainder) % 11;
            
            String checkChar = (checkDigit == 10) ? "X" : String.valueOf(checkDigit);
            return isbn10WithoutCheck + checkChar;
            
        } catch (Exception e) {
            log.warn("ISBN-13 to ISBN-10 변환 실패 - ISBN-13: {}", isbn13);
            return null;
        }
    }

    /**
     * ISBN-10을 ISBN-13으로 변환
     */
    private String convertIsbn10ToIsbn13(String isbn10) {
        try {
            // 978 접두사 추가 후 처음 9자리 추출
            String isbn13WithoutCheck = "978" + isbn10.substring(0, 9);
            
            // ISBN-13 체크 디지트 계산
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                int digit = Character.getNumericValue(isbn13WithoutCheck.charAt(i));
                int weight = (i % 2 == 0) ? 1 : 3;
                sum += digit * weight;
            }
            
            int remainder = sum % 10;
            int checkDigit = (10 - remainder) % 10;
            
            return isbn13WithoutCheck + checkDigit;
            
        } catch (Exception e) {
            log.warn("ISBN-10 to ISBN-13 변환 실패 - ISBN-10: {}", isbn10);
            return null;
        }
    }

    /**
     * 외부 API를 통한 ISBN 검증
     * 
     * 검증 가능한 외부 서비스:
     * - Google Books API
     * - Open Library API
     * - WorldCat API
     * - ISBN.org API
     * 
     * 구현 시 고려사항:
     * - API 키 관리
     * - 호출량 제한 대응
     * - 타임아웃 설정
     * - 캐싱 전략
     * - 장애 처리
     */
    private boolean verifyWithExternalAPI(String isbn) {
        try {
            // TODO: 실제 외부 API 호출 구현
            // 예시: Google Books API 호출
            // String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
            // RestTemplate restTemplate = new RestTemplate();
            // ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            // 
            // if (response.getStatusCode() == HttpStatus.OK) {
            //     JsonNode root = objectMapper.readTree(response.getBody());
            //     int totalItems = root.path("totalItems").asInt();
            //     return totalItems > 0;
            // }
            
            // 개발 단계에서는 모든 ISBN을 유효한 것으로 간주
            log.debug("외부 API ISBN 검증 (스텁) - ISBN: {}", maskIsbn(isbn));
            return true;
            
        } catch (Exception e) {
            log.warn("외부 API ISBN 검증 실패 - ISBN: {}, 오류: {}", maskIsbn(isbn), e.getMessage());
            return true; // 외부 서비스 실패 시 유효한 것으로 간주
        }
    }

    /**
     * 커스텀 오류 메시지 추가
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }

    /**
     * ISBN 마스킹 (로깅 시 개인정보 보호)
     * 
     * 마스킹 패턴:
     * - ISBN-10: 1234567890 → 123***7890
     * - ISBN-13: 1234567890123 → 123***890123
     */
    private String maskIsbn(String isbn) {
        if (isbn == null || isbn.length() < 6) {
            return "***";
        }
        
        if (isbn.length() == 10) {
            return isbn.substring(0, 3) + "***" + isbn.substring(7);
        } else if (isbn.length() == 13) {
            return isbn.substring(0, 3) + "***" + isbn.substring(9);
        } else {
            return isbn.substring(0, 3) + "***";
        }
    }
}