package com.alibou.booknetwork.utility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 엔터프라이즈급 암호화 유틸리티 클래스
 * 
 * 암호화의 엔터프라이즈 가치:
 * 1. 데이터 보호: 민감한 정보의 기밀성 보장
 * 2. 규정 준수: GDPR, HIPAA 등 법적 요구사항 충족
 * 3. 신뢰성 확보: 고객 데이터 보호를 통한 브랜드 신뢰도 향상
 * 4. 위험 관리: 데이터 유출 시 피해 최소화
 * 
 * SSGD에서 학습한 암호화 패턴:
 * - AES-256-GCM: 인증된 암호화로 무결성과 기밀성 동시 보장
 * - 키 관리: 환경변수를 통한 안전한 키 관리
 * - 솔트 사용: 무지개 테이블 공격 방어
 * - IV(Initialization Vector) 랜덤 생성: 동일 평문의 다른 암호문 보장
 * 
 * 보안 고려사항:
 * - 암호화 키는 소스코드에 하드코딩 금지
 * - HSM(Hardware Security Module) 연동 고려
 * - 키 순환(Key Rotation) 정책 수립
 * - 암호화된 데이터의 백업 및 복구 전략
 */
@Component
@Slf4j
public class CryptoUtil {

    // 암호화 알고리즘 상수
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HASH_ALGORITHM = "SHA-256";
    
    // GCM 파라미터
    private static final int GCM_IV_LENGTH = 16;    // GCM 표준 IV 길이
    private static final int GCM_TAG_LENGTH = 16;   // GCM 인증 태그 길이
    private static final int AES_KEY_SIZE = 256;    // AES-256 키 크기

    // 암호화 키 (실제 환경에서는 환경변수 또는 키 관리 시스템에서 관리)
    @Value("${app.crypto.secret-key:default-secret-key-change-in-production}")
    private String secretKeyString;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 문자열 암호화
     * 
     * AES-GCM 암호화의 특징:
     * - Galois/Counter Mode: 병렬 처리 가능한 고성능 모드
     * - 인증된 암호화: 암호화와 동시에 무결성 검증
     * - AEAD (Authenticated Encryption with Associated Data)
     * 
     * 암호화 프로세스:
     * 1. 랜덤 IV 생성 (재사용 방지)
     * 2. 평문을 AES-GCM으로 암호화
     * 3. IV + 암호문 + 인증태그를 Base64 인코딩
     * 
     * @param plainText 암호화할 평문
     * @return Base64 인코딩된 암호문 (IV + 암호문 + 인증태그)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            log.warn("빈 문자열 암호화 시도");
            return null;
        }

        try {
            // 1. 비밀키 준비
            SecretKeySpec secretKey = createSecretKey();
            
            // 2. 랜덤 IV 생성 (매번 다른 IV 사용으로 보안 강화)
            byte[] iv = generateIV();
            
            // 3. Cipher 초기화
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            
            // 4. 평문 암호화
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // 5. IV + 암호문을 결합하여 Base64 인코딩
            byte[] encryptedWithIv = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, encryptedWithIv, iv.length, encryptedBytes.length);
            
            String result = Base64.getEncoder().encodeToString(encryptedWithIv);
            log.debug("문자열 암호화 완료 - 원본 길이: {}, 암호화 결과 길이: {}", 
                plainText.length(), result.length());
                
            return result;
            
        } catch (Exception e) {
            log.error("문자열 암호화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("암호화 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 문자열 복호화
     * 
     * 복호화 프로세스:
     * 1. Base64 디코딩
     * 2. IV와 암호문 분리
     * 3. AES-GCM으로 복호화 (동시에 인증태그 검증)
     * 4. 평문 반환
     * 
     * @param encryptedText Base64 인코딩된 암호문
     * @return 복호화된 평문
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            log.warn("빈 문자열 복호화 시도");
            return null;
        }

        try {
            // 1. Base64 디코딩
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);
            
            if (encryptedWithIv.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("암호화된 데이터의 길이가 부족합니다.");
            }
            
            // 2. IV와 암호문 분리
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedBytes = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);
            
            // 3. 비밀키 준비
            SecretKeySpec secretKey = createSecretKey();
            
            // 4. Cipher 초기화
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            
            // 5. 복호화 (인증태그 자동 검증)
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String result = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            log.debug("문자열 복호화 완료 - 암호문 길이: {}, 복호화 결과 길이: {}", 
                encryptedText.length(), result.length());
                
            return result;
            
        } catch (Exception e) {
            log.error("문자열 복호화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("복호화 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 패스워드 해싱 (단방향)
     * 
     * 단방향 해싱의 용도:
     * - 패스워드 저장: 원본 패스워드를 저장하지 않고 해시값만 저장
     * - 무결성 검증: 데이터 변조 여부 확인
     * - 디지털 서명: 데이터 출처 및 무결성 보장
     * 
     * 솔트(Salt) 사용 이유:
     * - 무지개 테이블 공격 방어
     * - 동일한 패스워드의 다른 해시값 생성
     * - 사전 공격(Dictionary Attack) 방어
     * 
     * @param password 해싱할 패스워드
     * @param salt 솔트 값 (null인 경우 자동 생성)
     * @return 솔트 + 해시값을 Base64 인코딩한 문자열
     */
    public String hashPassword(String password, byte[] salt) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("패스워드는 비어있을 수 없습니다.");
        }

        try {
            // 솔트가 제공되지 않은 경우 랜덤 생성
            if (salt == null) {
                salt = generateSalt();
            }

            // SHA-256 해싱
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(salt);
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // 솔트 + 해시값 결합
            byte[] saltedHash = new byte[salt.length + hashedBytes.length];
            System.arraycopy(salt, 0, saltedHash, 0, salt.length);
            System.arraycopy(hashedBytes, 0, saltedHash, salt.length, hashedBytes.length);

            String result = Base64.getEncoder().encodeToString(saltedHash);
            log.debug("패스워드 해싱 완료 - 솔트 길이: {}, 해시 길이: {}", salt.length, hashedBytes.length);
            
            return result;

        } catch (Exception e) {
            log.error("패스워드 해싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("패스워드 해싱 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 패스워드 검증
     * 
     * @param password 검증할 패스워드
     * @param hashedPassword 저장된 해시값 (솔트 포함)
     * @return 패스워드 일치 여부
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        if (password == null || hashedPassword == null) {
            return false;
        }

        try {
            // 저장된 해시값에서 솔트 추출
            byte[] saltedHash = Base64.getDecoder().decode(hashedPassword);
            
            if (saltedHash.length < 32) { // 최소 솔트(16) + SHA-256 해시(32) = 48
                log.warn("해시값의 길이가 부족합니다.");
                return false;
            }

            // 솔트 추출 (처음 16바이트)
            byte[] salt = new byte[16];
            System.arraycopy(saltedHash, 0, salt, 0, 16);

            // 입력된 패스워드를 같은 솔트로 해싱
            String newHash = hashPassword(password, salt);

            // 해시값 비교 (타이밍 공격 방어를 위해 상수 시간 비교)
            return constantTimeEquals(hashedPassword, newHash);

        } catch (Exception e) {
            log.error("패스워드 검증 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 랜덤 토큰 생성
     * 
     * 용도:
     * - API 키 생성
     * - 세션 토큰
     * - 임시 패스워드
     * - CSRF 토큰
     * 
     * @param length 토큰 길이 (바이트)
     * @return Base64 인코딩된 랜덤 토큰
     */
    public String generateRandomToken(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("토큰 길이는 0보다 커야 합니다.");
        }

        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        log.debug("랜덤 토큰 생성 완료 - 길이: {}", token.length());
        
        return token;
    }

    /**
     * 데이터 무결성 검증을 위한 체크섬 생성
     * 
     * @param data 체크섬을 생성할 데이터
     * @return SHA-256 해시값의 Base64 인코딩
     */
    public String generateChecksum(String data) {
        if (data == null) {
            return null;
        }

        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
            
        } catch (Exception e) {
            log.error("체크섬 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("체크섬 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 비밀키 생성
     * 
     * 키 관리 보안 원칙:
     * 1. 키는 소스코드에 하드코딩하지 않음
     * 2. 환경변수 또는 외부 키 관리 시스템 사용
     * 3. 키 순환 정책 수립
     * 4. 키 접근 권한 최소화
     */
    private SecretKeySpec createSecretKey() {
        try {
            // 설정된 키를 SHA-256으로 해싱하여 32바이트 키 생성
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] keyBytes = digest.digest(secretKeyString.getBytes(StandardCharsets.UTF_8));
            
            return new SecretKeySpec(keyBytes, ALGORITHM);
            
        } catch (Exception e) {
            log.error("비밀키 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("비밀키 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 랜덤 IV(Initialization Vector) 생성
     * 
     * IV의 중요성:
     * - 동일한 평문이 다른 암호문으로 암호화됨
     * - 패턴 분석 공격 방어
     * - CBC, GCM 등 모드에서 필수
     */
    private byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * 랜덤 솔트 생성
     */
    private byte[] generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * 상수 시간 문자열 비교 (타이밍 공격 방어)
     * 
     * 타이밍 공격:
     * - 비교 시간 차이로 정보 유출
     * - 문자별 비교 시간 측정으로 패스워드 추측
     * - 상수 시간 비교로 방어
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }

    /**
     * 암호화 키 검증 (개발/테스트용)
     * 
     * 프로덕션 환경에서는 다음을 확인:
     * - 키 길이가 충분한지
     * - 기본값을 사용하고 있지는 않은지
     * - 키가 안전하게 저장되어 있는지
     */
    public boolean validateEncryptionSetup() {
        try {
            // 기본 키 사용 여부 확인
            if ("default-secret-key-change-in-production".equals(secretKeyString)) {
                log.warn("기본 암호화 키가 사용되고 있습니다. 프로덕션 환경에서는 반드시 변경해야 합니다.");
                return false;
            }
            
            // 키 길이 확인
            if (secretKeyString.length() < 32) {
                log.warn("암호화 키의 길이가 너무 짧습니다. 최소 32자 이상 권장합니다.");
                return false;
            }
            
            // 간단한 암호화/복호화 테스트
            String testMessage = "encryption-test";
            String encrypted = encrypt(testMessage);
            String decrypted = decrypt(encrypted);
            
            if (!testMessage.equals(decrypted)) {
                log.error("암호화/복호화 테스트 실패");
                return false;
            }
            
            log.info("암호화 설정 검증 완료");
            return true;
            
        } catch (Exception e) {
            log.error("암호화 설정 검증 실패: {}", e.getMessage(), e);
            return false;
        }
    }
}