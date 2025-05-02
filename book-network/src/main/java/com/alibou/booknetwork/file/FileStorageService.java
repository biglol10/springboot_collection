package com.alibou.booknetwork.file;

import com.alibou.booknetwork.book.Book;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.io.File.separator;
import static java.lang.System.currentTimeMillis;

/**
 * 파일 저장 서비스
 * 
 * 이 서비스는 클라이언트로부터 업로드된 파일을 서버의 파일 시스템에 저장하는 기능을 제공합니다.
 * 주로 사용자 관련 파일(예: 프로필 사진)을 저장하는 데 사용됩니다.
 * 
 * 주요 기능:
 * - 파일 업로드 및 저장
 * - 사용자별 디렉토리 구조 자동 생성
 * - 파일명 충돌 방지를 위한 타임스탬프 기반 이름 생성
 */
@Service
@Slf4j // Lombok이 제공하는 로깅 기능을 사용하기 위한 어노테이션
@RequiredArgsConstructor // 필수 필드를 포함하는 생성자를 자동으로 생성
public class FileStorageService {
    @Value("${application.file.upload.photos-output-path}")
    private String fileUploadPath; // 설정 파일에서 정의된 파일 업로드 기본 경로

    /**
     * 사용자 관련 파일을 저장합니다.
     * 
     * 이 메소드는 사용자 ID를 기반으로 적절한 디렉토리 구조를 생성하고,
     * 업로드된 파일을 해당 디렉토리에 저장합니다.
     * 
     * @param sourceFile 업로드된 파일 객체
     * @param userId 파일을 소유한 사용자의 ID
     * @return 저장된 파일의 경로 (성공 시) 또는 null (실패 시)
     */
    public String saveFile(@NonNull MultipartFile sourceFile,
                           @NonNull Integer userId) { // 반환 값은 저장된 파일의 경로입니다

        // File.separator를 사용하여 플랫폼 독립적인 경로 구성 (윈도우, 리눅스 등)
        final String fileUploadSubPath = "users" + separator + userId;
        return uploadFile(sourceFile, fileUploadSubPath);
    }

    /**
     * 파일을 지정된 하위 경로에 업로드합니다.
     * 
     * 이 private 메소드는 실제 파일 저장 로직을 처리합니다:
     * 1. 대상 디렉토리가 존재하지 않으면 생성
     * 2. 파일 확장자 추출
     * 3. 고유한 파일명 생성 (현재 시간 밀리초 + 확장자)
     * 4. 파일 저장 및 결과 반환
     * 
     * @param sourceFile 업로드된 파일 객체
     * @param fileUploadSubPath 파일을 저장할 하위 경로
     * @return 저장된 파일의 전체 경로 또는 실패 시 null
     */
    private String uploadFile(@NonNull MultipartFile sourceFile,
                              @NonNull String fileUploadSubPath) {
        // 최종 업로드 경로 구성 (기본 경로 + 하위 경로)
        final String finalUploadPath = fileUploadPath + separator + fileUploadSubPath;
        
        // 대상 폴더 객체 생성
        File targetFoler = new File(finalUploadPath);
        
        // 폴더가 존재하지 않으면 생성
        if (!targetFoler.exists()) {
            boolean folderCreated = targetFoler.mkdirs(); // mkdirs는 경로 상의 모든 하위 폴더를 함께 생성합니다
            if (!folderCreated) {
                log.warn("Failed to create the target folder");
                return null; // 폴더 생성 실패 시 null 반환
            }
        }

        // 파일 확장자 추출
        final String fileExtension = getFileExtension(sourceFile.getOriginalFilename());
        
        // 타임스탬프를 이용한 고유 파일명 생성 (예: ./upload/users/1/1634567890123.jpg)
        String targetFilePath = finalUploadPath + separator + currentTimeMillis() + "." + fileExtension;

        // 파일 저장을 위한 Path 객체 생성
        Path targetPath = Paths.get(targetFilePath);
        
        try {
            // 파일 바이트를 대상 경로에 쓰기
            Files.write(targetPath, sourceFile.getBytes());
            log.info("File saved to " + targetFilePath);
            return targetFilePath; // 성공 시 파일 경로 반환
        } catch (IOException e) {
            log.error("File was not saved", e);
        }
        return null; // 예외 발생 시 null 반환
    }

    /**
     * 파일 이름에서 확장자를 추출합니다.
     * 
     * @param fileName 파일 이름
     * @return 소문자로 변환된 파일 확장자 또는 확장자가 없는 경우 빈 문자열
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        // 마지막 점(.)의 위치 찾기
        int lastDotIndex = fileName.lastIndexOf(".");
        
        if (lastDotIndex == -1) { // 확장자가 없는 경우
            return "";
        }

        // 마지막 점 이후의 문자열을 소문자로 반환
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 보안 강화:
 *    - 파일 유형 검증: MIME 타입 확인으로 악성 파일 업로드 방지
 *    - 파일 크기 제한: 대용량 파일로 인한 서버 부하 방지
 *    - 파일 스캐닝: 바이러스/악성코드 검사 통합
 *    
 *    예시:
 *    private void validateFile(MultipartFile file) {
 *        // MIME 타입 확인
 *        String contentType = file.getContentType();
 *        if (!allowedTypes.contains(contentType)) {
 *            throw new InvalidFileTypeException();
 *        }
 *        
 *        // 파일 크기 확인
 *        if (file.getSize() > maxFileSize) {
 *            throw new FileTooLargeException();
 *        }
 *    }
 * 
 * 2. 클라우드 스토리지 통합:
 *    - AWS S3, Google Cloud Storage, Azure Blob Storage 등 통합
 *    - 멀티 클라우드 전략을 위한 스토리지 추상화 계층 구현
 *    
 * 3. 이미지 처리 기능:
 *    - 업로드 시 이미지 리사이징/크롭
 *    - 썸네일 자동 생성
 *    - 이미지 최적화 (압축, 포맷 변환)
 *    
 * 4. 메타데이터 관리:
 *    - 파일 메타데이터 추출 및 데이터베이스 저장
 *    - 태그 및 카테고리 시스템 구현
 *    - 검색 기능 통합
 * 
 * 5. 분산 파일 시스템:
 *    - 고가용성을 위한 파일 복제
 *    - 샤딩(Sharding) 전략 구현
 *    - CDN 통합
 * 
 * 6. 트랜잭션 처리:
 *    - 파일 저장과 데이터베이스 업데이트를 단일 트랜잭션으로 처리
 *    - 실패 시 롤백 메커니즘 구현
 *    
 * 7. 비동기 처리:
 *    - 대용량 파일 업로드를 위한 비동기 처리
 *    - 진행 상태 모니터링 및 알림
 *    - 백그라운드 처리 작업 큐 구현
 * 
 * 8. 접근 제어:
 *    - 파일 접근 권한 관리
 *    - 서명된 URL을 통한 임시 접근 제공
 *    - 만료 정책 구현
 */
