package com.alibou.booknetwork.service.async;

import com.alibou.booknetwork.email.EmailService;
import com.alibou.booknetwork.email.EmailTemplateName;
import com.alibou.booknetwork.user.User;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 비동기 이메일 처리 서비스
 * 
 * 핵심 개념:
 * 1. @Async: Spring의 비동기 처리 어노테이션
 * 2. CompletableFuture: Java 8+의 비동기 프로그래밍 API
 * 3. Non-blocking: 메인 쓰레드를 차단하지 않는 실행
 * 
 * 엔터프라이즈 의미:
 * - 사용자 경험 향상: 이메일 발송 대기 시간 제거
 * - 시스템 처리량 증가: 동시 다중 요청 처리 가능
 * - 장애 격리: 이메일 발송 실패가 메인 프로세스에 영향 없음
 * 
 * SSGD 패턴 적용:
 * - ThreadPoolTaskExecutor 사용으로 쓰레드 풀 관리
 * - 예외 처리를 통한 안정성 확보
 * - 로깅을 통한 모니터링 가능
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailService {

    private final EmailService emailService;

    /**
     * 비동기 계정 활성화 이메일 발송
     * 
     * 동작 원리:
     * 1. @Async 어노테이션으로 별도 쓰레드에서 실행
     * 2. CompletableFuture로 결과를 비동기적으로 반환
     * 3. 메인 쓰레드는 즉시 다음 작업 수행 가능
     * 
     * 왜 필요한가:
     * - 회원가입 프로세스에서 이메일 발송은 부가 기능
     * - 이메일 서버 응답 대기로 인한 사용자 경험 저하 방지
     * - SMTP 서버 장애 시에도 회원가입 프로세스는 정상 완료
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendActivationEmailAsync(
            String to,
            String username,
            EmailTemplateName emailTemplate,
            String confirmationUrl,
            String activationCode,
            String subject
    ) {
        log.info("비동기 이메일 발송 시작 - 수신자: {}, 템플릿: {}", to, emailTemplate);
        
        try {
            emailService.sendEmail(
                to,
                username,
                emailTemplate,
                confirmationUrl,
                activationCode,
                subject
            );
            
            log.info("이메일 발송 성공 - 수신자: {}", to);
            return CompletableFuture.completedFuture(true);
            
        } catch (MessagingException e) {
            log.error("이메일 발송 실패 - 수신자: {}, 오류: {}", to, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 비동기 대량 이메일 발송 (관리자용)
     * 
     * 엔터프라이즈 패턴:
     * - 대량 처리 시 배치 단위로 분할
     * - 실패한 이메일만 별도 처리
     * - 진행률 모니터링 가능
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Integer> sendBulkEmailAsync(
            java.util.List<User> users,
            EmailTemplateName template,
            String subject,
            String content
    ) {
        log.info("대량 이메일 발송 시작 - 대상 사용자 수: {}", users.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (User user : users) {
            try {
                emailService.sendEmail(
                    user.getEmail(),
                    user.getFullName(),
                    template,
                    content,
                    "",
                    subject
                );
                successCount++;
                
                // 이메일 서버 부하 방지를 위한 지연
                TimeUnit.MILLISECONDS.sleep(100);
                
            } catch (Exception e) {
                failureCount++;
                log.error("대량 이메일 발송 실패 - 사용자: {}, 오류: {}", 
                    user.getEmail(), e.getMessage());
            }
        }
        
        log.info("대량 이메일 발송 완료 - 성공: {}, 실패: {}", successCount, failureCount);
        return CompletableFuture.completedFuture(successCount);
    }

    /**
     * 이메일 발송 상태 확인 (콜백 방식)
     * 
     * 고급 패턴:
     * - CompletableFuture 체이닝을 통한 후속 작업 처리
     * - thenApply, thenCompose, exceptionally 등 활용
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<String> sendEmailWithCallback(
            String to,
            String username,
            EmailTemplateName template,
            String confirmationUrl,
            String activationCode,
            String subject
    ) {
        return CompletableFuture
            .supplyAsync(() -> {
                try {
                    emailService.sendEmail(to, username, template, confirmationUrl, activationCode, subject);
                    return "SUCCESS";
                } catch (MessagingException e) {
                    throw new RuntimeException("이메일 발송 실패: " + e.getMessage(), e);
                }
            })
            .thenApply(result -> {
                log.info("이메일 발송 완료 후 처리 - 결과: {}", result);
                // 여기서 추가적인 후속 작업 수행 가능 (예: 통계 업데이트)
                return result;
            })
            .exceptionally(throwable -> {
                log.error("이메일 발송 예외 처리: {}", throwable.getMessage());
                return "FAILED: " + throwable.getMessage();
            });
    }
}