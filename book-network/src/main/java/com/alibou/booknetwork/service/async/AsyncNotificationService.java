package com.alibou.booknetwork.service.async;

import com.alibou.booknetwork.book.Book;
import com.alibou.booknetwork.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 비동기 알림 처리 서비스
 * 
 * 설계 원칙:
 * 1. 단일 책임 원칙: 알림 발송만 담당
 * 2. 개방-폐쇄 원칙: 새로운 알림 타입 추가 용이
 * 3. 의존성 역전 원칙: 인터페이스를 통한 느슨한 결합
 * 
 * 비동기 처리의 핵심 가치:
 * - 응답성(Responsiveness): 사용자 액션에 즉시 응답
 * - 탄력성(Resilience): 알림 시스템 장애가 주 기능에 영향 없음
 * - 확장성(Scalability): 대량의 알림을 효율적으로 처리
 * 
 * SSGD에서 학습한 패턴:
 * - ThreadPoolTaskExecutor를 통한 쓰레드 풀 관리
 * - CompletableFuture의 조합을 통한 복합 비동기 작업
 * - 로깅과 모니터링을 통한 운영 가시성 확보
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncNotificationService {

    /**
     * 도서 대여 알림 비동기 처리
     * 
     * 비즈니스 시나리오:
     * - 사용자가 도서를 대여할 때
     * - 도서 소유자에게 알림 발송
     * - 대여자에게 확인 알림 발송
     * 
     * 비동기 처리 이유:
     * 1. 도서 대여 트랜잭션과 알림 발송을 분리
     * 2. 알림 발송 실패가 대여 프로세스에 영향 없음
     * 3. 다중 알림을 병렬로 처리하여 성능 향상
     */
    @Async("notificationTaskExecutor")
    public CompletableFuture<Void> sendBookBorrowNotification(
            Book book, 
            User borrower, 
            User owner
    ) {
        log.info("도서 대여 알림 발송 시작 - 도서: {}, 대여자: {}, 소유자: {}", 
            book.getTitle(), borrower.getFullName(), owner.getFullName());

        try {
            // 소유자에게 알림
            CompletableFuture<Void> ownerNotification = CompletableFuture.runAsync(() -> {
                sendNotificationToUser(
                    owner,
                    "도서 대여 알림",
                    String.format("회원 %s님이 '%s' 도서를 대여했습니다.", 
                        borrower.getFullName(), book.getTitle()),
                    "BOOK_BORROWED"
                );
            });

            // 대여자에게 확인 알림
            CompletableFuture<Void> borrowerNotification = CompletableFuture.runAsync(() -> {
                sendNotificationToUser(
                    borrower,
                    "도서 대여 확인",
                    String.format("'%s' 도서 대여가 완료되었습니다. 반납 예정일을 확인해 주세요.", 
                        book.getTitle()),
                    "BOOK_BORROW_CONFIRMED"
                );
            });

            // 두 알림이 모두 완료될 때까지 대기
            CompletableFuture.allOf(ownerNotification, borrowerNotification).join();
            
            log.info("도서 대여 알림 발송 완료 - 도서: {}", book.getTitle());
            
        } catch (Exception e) {
            log.error("도서 대여 알림 발송 실패 - 도서: {}, 오류: {}", book.getTitle(), e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 도서 반납 알림 비동기 처리
     * 
     * 고급 CompletableFuture 패턴 적용:
     * - thenCompose: 순차적 비동기 작업 체이닝
     * - thenCombine: 병렬 비동기 작업 결합
     * - whenComplete: 완료/예외 상황 모두 처리
     */
    @Async("notificationTaskExecutor")
    public CompletableFuture<Boolean> sendBookReturnNotification(
            Book book, 
            User borrower, 
            User owner
    ) {
        log.info("도서 반납 알림 발송 시작 - 도서: {}", book.getTitle());

        return CompletableFuture
            .supplyAsync(() -> {
                // 반납 확인 알림 (대여자)
                return sendNotificationToUser(
                    borrower,
                    "도서 반납 완료",
                    String.format("'%s' 도서가 성공적으로 반납되었습니다.", book.getTitle()),
                    "BOOK_RETURNED"
                );
            })
            .thenCompose(borrowerResult -> {
                // 반납 알림 (소유자)
                return CompletableFuture.supplyAsync(() -> {
                    return sendNotificationToUser(
                        owner,
                        "도서 반납 알림",
                        String.format("대여하신 '%s' 도서가 반납되었습니다.", book.getTitle()),
                        "BOOK_RETURN_RECEIVED"
                    );
                }).thenCombine(CompletableFuture.completedFuture(borrowerResult), 
                    (ownerResult, prevResult) -> ownerResult && prevResult);
            })
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("도서 반납 알림 발송 중 예외 발생: {}", throwable.getMessage(), throwable);
                } else {
                    log.info("도서 반납 알림 발송 완료 - 결과: {}", result ? "성공" : "실패");
                }
            });
    }

    /**
     * 피드백 알림 비동기 처리 (평점/리뷰 등록 시)
     * 
     * 실시간 알림의 중요성:
     * - 사용자 참여도 향상
     * - 커뮤니티 활성화
     * - 즉각적인 피드백 루프 형성
     */
    @Async("notificationTaskExecutor")
    public CompletableFuture<Void> sendFeedbackNotification(
            Book book, 
            User reviewer, 
            User bookOwner,
            String feedbackType,
            Double rating
    ) {
        log.info("피드백 알림 발송 시작 - 도서: {}, 리뷰어: {}, 타입: {}", 
            book.getTitle(), reviewer.getFullName(), feedbackType);

        return CompletableFuture.runAsync(() -> {
            try {
                String message = createFeedbackMessage(book.getTitle(), reviewer.getFullName(), 
                    feedbackType, rating);
                
                sendNotificationToUser(
                    bookOwner,
                    "새로운 피드백 도착",
                    message,
                    "FEEDBACK_RECEIVED"
                );
                
                // 지연을 두어 시스템 부하 방지
                TimeUnit.MILLISECONDS.sleep(50);
                
            } catch (Exception e) {
                log.error("피드백 알림 발송 실패: {}", e.getMessage(), e);
                throw new RuntimeException("피드백 알림 발송 실패", e);
            }
        });
    }

    /**
     * 시스템 알림 일괄 발송 (관리자용)
     * 
     * 대용량 처리 패턴:
     * - 배치 크기 제한을 통한 메모리 관리
     * - 병렬 처리를 통한 성능 최적화
     * - 실패 처리 및 재시도 로직
     */
    @Async("notificationTaskExecutor")
    public CompletableFuture<Integer> sendSystemNotificationToAll(
            java.util.List<User> users,
            String title,
            String message,
            String type
    ) {
        log.info("시스템 알림 일괄 발송 시작 - 대상 사용자 수: {}", users.size());
        
        final int BATCH_SIZE = 100;
        int totalSuccessCount = 0;
        
        // 배치 단위로 처리
        for (int i = 0; i < users.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, users.size());
            java.util.List<User> batchUsers = users.subList(i, endIndex);
            
            // 배치 내 병렬 처리
            java.util.List<CompletableFuture<Boolean>> futures = batchUsers.stream()
                .map(user -> CompletableFuture.supplyAsync(() -> 
                    sendNotificationToUser(user, title, message, type)))
                .toList();
            
            // 배치 완료 대기
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            
            try {
                allOf.get(30, TimeUnit.SECONDS); // 30초 타임아웃
                
                // 성공한 알림 수 계산
                long batchSuccessCount = futures.stream()
                    .mapToLong(future -> {
                        try {
                            return future.get() ? 1L : 0L;
                        } catch (Exception e) {
                            return 0L;
                        }
                    })
                    .sum();
                
                totalSuccessCount += batchSuccessCount;
                log.info("배치 처리 완료 - 배치 크기: {}, 성공: {}", 
                    batchUsers.size(), batchSuccessCount);
                
            } catch (Exception e) {
                log.error("배치 처리 실패 - 배치 인덱스: {}, 오류: {}", i / BATCH_SIZE, e.getMessage());
            }
        }
        
        log.info("시스템 알림 일괄 발송 완료 - 총 성공: {}/{}", totalSuccessCount, users.size());
        return CompletableFuture.completedFuture(totalSuccessCount);
    }

    /**
     * 실제 알림 발송 구현 (스텁)
     * 
     * 실제 환경에서는:
     * - Push 알림 서비스 연동 (FCM, APNS)
     * - 웹소켓을 통한 실시간 알림
     * - 이메일/SMS 연동
     * - 데이터베이스 알림 이력 저장
     */
    private boolean sendNotificationToUser(User user, String title, String message, String type) {
        try {
            log.info("알림 발송 - 사용자: {}, 제목: {}, 타입: {}", 
                user.getFullName(), title, type);
            
            // 실제 알림 발송 로직 구현
            // 예: pushNotificationService.send(user.getDeviceToken(), title, message);
            // 예: webSocketService.sendToUser(user.getId(), notification);
            // 예: notificationRepository.save(new Notification(user, title, message, type));
            
            // 시뮬레이션을 위한 지연
            TimeUnit.MILLISECONDS.sleep(10);
            
            return true;
            
        } catch (Exception e) {
            log.error("알림 발송 실패 - 사용자: {}, 오류: {}", user.getFullName(), e.getMessage());
            return false;
        }
    }

    /**
     * 피드백 메시지 생성 헬퍼 메서드
     */
    private String createFeedbackMessage(String bookTitle, String reviewerName, 
                                       String feedbackType, Double rating) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("%s님이 '%s' 도서에 ", reviewerName, bookTitle));
        
        switch (feedbackType.toUpperCase()) {
            case "REVIEW":
                message.append("리뷰를 작성했습니다.");
                break;
            case "RATING":
                message.append(String.format("별점 %.1f점을 주었습니다.", rating));
                break;
            case "COMMENT":
                message.append("댓글을 남겼습니다.");
                break;
            default:
                message.append("피드백을 남겼습니다.");
        }
        
        return message.toString();
    }
}