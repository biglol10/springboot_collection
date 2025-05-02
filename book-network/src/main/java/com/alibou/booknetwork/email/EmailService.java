package com.alibou.booknetwork.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 이메일 서비스
 * 
 * 이 서비스는 애플리케이션 내에서 사용자에게 이메일을 발송하는 기능을 제공합니다.
 * Thymeleaf 템플릿 엔진을 사용하여 HTML 이메일을 생성하고,
 * JavaMailSender를 통해 비동기 방식으로 발송합니다.
 * 
 * 주요 기능:
 * - 템플릿 기반 HTML 이메일 생성
 * - 비동기 이메일 발송으로 응답 시간 개선
 * - 계정 활성화, 알림 등 다양한 용도의 이메일 지원
 */
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    /**
     * 이메일을 비동기적으로 발송합니다.
     * 
     * @Async 어노테이션을 통해 별도의 스레드에서 실행되므로,
     * 이메일 발송으로 인해 사용자 응답이 지연되지 않습니다.
     * 메인 애플리케이션 클래스에서 비동기 처리 활성화가 필요합니다.
     * 
     * @param to 수신자 이메일 주소
     * @param username 수신자 이름
     * @param emailTemplateName 사용할 이메일 템플릿
     * @param confirmationUrl 확인 URL (예: 계정 활성화 링크)
     * @param activationCode 활성화 코드
     * @param subject 이메일 제목
     * @throws MessagingException 이메일 발송 중 오류 발생 시
     */
    @Async
    public void sendEmail(String to,
                          String username,
                          EmailTemplateName emailTemplateName,
                          String confirmationUrl,
                          String activationCode,
                          String subject) throws MessagingException {

        // 템플릿 이름 결정 - 기본값은 "confirm-email"
        String templateName;
        if (emailTemplateName == null) {
            templateName = "confirm-email";
        } else {
            templateName = emailTemplateName.name();
        }

        // MIME 메시지 생성
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED,
                StandardCharsets.UTF_8.name());

        // 템플릿에 전달할 변수 설정
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("confirmationUrl", confirmationUrl);
        properties.put("activation_code", activationCode);

        // Thymeleaf 컨텍스트 설정
        Context context = new Context();
        context.setVariables(properties);

        // 이메일 기본 정보 설정
        helper.setFrom("contact@aliboucoding.com");
        helper.setTo(to);
        helper.setSubject(subject);

        // 템플릿 처리하여 HTML 이메일 본문 생성
        String template = templateEngine.process(templateName, context);

        // 이메일 본문 설정 (true는 HTML 형식을 의미)
        helper.setText(template, true);

        // 이메일 발송
        mailSender.send(mimeMessage);
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 다양한 이메일 유형 지원:
 *    - 다양한 알림 유형별 템플릿 및 전송 로직 구현
 *    - 마케팅 이메일과 트랜잭션 이메일 분리
 *    - 이메일 구독 및 수신 거부 관리
 *    
 *    예시:
 *    public enum EmailCategory {
 *        TRANSACTIONAL, MARKETING, NEWSLETTER, NOTIFICATION
 *    }
 *    
 *    public void sendEmail(String to, EmailType type, Map<String, Object> data) {
 *        // 이메일 유형에 따른 템플릿 및 발송 로직
 *    }
 * 
 * 2. 이메일 전송 추적 및 분석:
 *    - 발송, 열람, 클릭 등 이메일 상태 추적
 *    - 전송 실패 시 자동 재시도 메커니즘
 *    - 분석 지표 수집 및 보고
 *    
 *    예시:
 *    @Entity
 *    public class EmailLog {
 *        @Id @GeneratedValue
 *        private Long id;
 *        private String recipient;
 *        private EmailType type;
 *        private EmailStatus status;
 *        private LocalDateTime sentAt;
 *        private LocalDateTime openedAt;
 *        private String trackingId;
 *        // ...
 *    }
 * 
 * 3. 전송 제공자 추상화:
 *    - 다양한 이메일 서비스 제공자(AWS SES, SendGrid, Mailgun 등) 지원
 *    - 전략 패턴을 활용한 제공자 전환 용이성
 *    - 장애 대응을 위한 대체 제공자 자동 전환
 *    
 *    예시:
 *    public interface EmailProvider {
 *        void send(EmailRequest request) throws EmailException;
 *    }
 *    
 *    @Service
 *    public class SendGridProvider implements EmailProvider {
 *        // SendGrid 구현
 *    }
 * 
 * 4. 최적화 및 확장성:
 *    - 대량 이메일 발송을 위한 배치 처리
 *    - 이메일 큐 시스템 구현 (RabbitMQ, Kafka 등)
 *    - 동시 발송 수 제한 및 속도 제어
 *    
 *    예시:
 *    @Async
 *    public CompletableFuture<Void> sendBulkEmail(List<String> recipients, EmailTemplate template) {
 *        return CompletableFuture.runAsync(() -> {
 *            recipients.forEach(recipient -> emailQueue.add(new EmailTask(recipient, template)));
 *        });
 *    }
 * 
 * 5. 템플릿 관리 고도화:
 *    - 관리자 UI를 통한 템플릿 편집
 *    - 버전 관리 및 A/B 테스트
 *    - 다국어 지원 및 현지화
 *    
 *    예시:
 *    @Entity
 *    public class EmailTemplate {
 *        @Id @GeneratedValue
 *        private Long id;
 *        private String name;
 *        private String content;
 *        private boolean active;
 *        @Version
 *        private Integer version;
 *        // ...
 *    }
 * 
 * 6. 이메일 검증 및 보안:
 *    - 수신자 이메일 주소 유효성 검증
 *    - SPF, DKIM, DMARC 설정 지원
 *    - 이메일 스푸핑 방지 메커니즘
 *    
 *    예시:
 *    public boolean validateEmail(String email) {
 *        // 정규식 검증, DNS 확인, SMTP 서버 확인 등
 *    }
 * 
 * 7. 예약 발송 기능:
 *    - 특정 시간에 이메일 발송 예약
 *    - 시간대 고려 및 최적 발송 시간 자동화
 *    - 반복 발송 일정 관리
 *    
 *    예시:
 *    public void scheduleEmail(String to, EmailType type, Map<String, Object> data, LocalDateTime scheduledTime) {
 *        // 예약 정보 저장 및 스케줄러 등록
 *    }
 * 
 * 8. 고급 컨텐츠 처리:
 *    - 동적 이미지 생성 및 포함
 *    - 개인화된 컨텐츠 생성 (사용자 행동 기반)
 *    - 첨부 파일 처리 및 보안 검사
 *    
 *    예시:
 *    public void sendEmailWithAttachment(String to, String subject, EmailTemplate template, File attachment) {
 *        // 첨부 파일 처리 로직
 *    }
 */
