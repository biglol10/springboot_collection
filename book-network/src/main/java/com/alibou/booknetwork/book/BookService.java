package com.alibou.booknetwork.book;

import com.alibou.booknetwork.common.PageResponse;
import com.alibou.booknetwork.exception.OperationNotPermittedException;
import com.alibou.booknetwork.file.FileStorageService;
import com.alibou.booknetwork.history.BookTransactionHistory;
import com.alibou.booknetwork.history.BookTransactionHistoryRepository;
import com.alibou.booknetwork.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

import static com.alibou.booknetwork.book.BookSpecification.withOwnerId;

/**
 * 도서 관리 서비스
 * 
 * 이 서비스는 도서 관련 비즈니스 로직을 처리합니다.
 * 주요 기능으로는 도서 등록, 조회, 공유 상태 관리, 대여 및 반납 처리 등이 있습니다.
 * 사용자 인증 정보를 기반으로 도서 소유자 확인 및 권한 검증을 수행합니다.
 */
@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final BookTransactionHistoryRepository transactionHistoryRepository;
    private final FileStorageService fileStorageService;

    /**
     * 새 도서를 등록합니다.
     * 
     * @param request 도서 정보가 담긴 요청 객체
     * @param connectedUser 현재 인증된 사용자
     * @return 저장된 도서의 ID
     */
    public Integer save(BookRequest request, Authentication connectedUser) {
        // getPrincipal()은 현재 인증된 사용자를 반환합니다.
        // User 클래스는 UserDetails와 Principal 인터페이스를 구현하므로 User로 캐스팅 가능합니다.
        User user = (User) connectedUser.getPrincipal();
        
        // Principal: 사용자의 신원을 나타내는 인터페이스로, 주로 사용자명과 같은 기본 정보를 제공합니다.
        // UserDetails: Spring Security에서 사용하는 사용자 상세 정보 인터페이스로, 사용자명, 비밀번호, 권한 등을 포함합니다.
        // Spring Security에서는 인증된 사용자의 UserDetails 객체가 SecurityContext에 저장되며,
        // 이후 Authentication 객체를 통해 해당 정보에 접근할 수 있습니다.

        Book book = bookMapper.toBook(request);
        book.setOwner(user);
        return bookRepository.save(book).getId();
    }

    /**
     * ID로 도서를 조회합니다.
     * 
     * @param bookId 조회할 도서 ID
     * @return 도서 응답 객체
     * @throws EntityNotFoundException 도서가 존재하지 않을 경우 발생
     */
    public BookResponse findById(Integer bookId) {
        return bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));
    }

    /**
     * 공유 가능한 모든 도서 목록을 페이징하여 조회합니다.
     * 현재 사용자가 소유한 도서는 제외됩니다.
     * 
     * @param page 페이지 번호
     * @param size 페이지당 항목 수
     * @param connectedUser 현재 인증된 사용자
     * @return 페이징된 도서 응답 객체
     */
    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        // 생성일 기준 내림차순으로 정렬된 페이지 요청 객체 생성
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // 공유 가능하고 보관 상태가 아니며, 현재 사용자가 소유하지 않은 도서 조회
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, user.getId());
        List<BookResponse> bookResponses = books.map(bookMapper::toBookResponse).toList();

        // 페이지 정보와 함께 응답 객체 생성
        return new PageResponse<>(
                bookResponses,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    /**
     * 현재 사용자가 소유한 도서 목록을 페이징하여 조회합니다.
     * 
     * @param page 페이지 번호
     * @param size 페이지당 항목 수
     * @param connectedUser 현재 인증된 사용자
     * @return 페이징된 도서 응답 객체
     */
    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // JpaSpecificationExecutor 인터페이스를 사용한 동적 쿼리 수행
        // withOwnerId는 BookSpecification에 정의된 명세(Specification)입니다.
        Page<Book> books = bookRepository.findAll(withOwnerId(user.getId()), pageable);

        List<BookResponse> booksResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                booksResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    /**
     * 현재 사용자가 대여 중인 도서 목록을 페이징하여 조회합니다.
     * 
     * @param page 페이지 번호
     * @param size 페이지당 항목 수
     * @param connectedUser 현재 인증된 사용자
     * @return 페이징된 대여 도서 응답 객체
     */
    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        
        // 현재 사용자가 대여 중인 도서 거래 내역 조회
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findAllBorrowedBooks(pageable, user.getId());
        List<BorrowedBookResponse> booksResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                booksResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    /**
     * 현재 사용자가 반납한 도서 목록을 페이징하여 조회합니다.
     * 
     * @param page 페이지 번호
     * @param size 페이지당 항목 수
     * @param connectedUser 현재 인증된 사용자
     * @return 페이징된 반납 도서 응답 객체
     */
    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        
        // 현재 사용자가 반납한 도서 거래 내역 조회
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findAllReturnedBooks(pageable, user.getId());
        List<BorrowedBookResponse> booksResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                booksResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    /**
     * 도서의 공유 가능 상태를 토글합니다.
     * 도서 소유자만 이 작업을 수행할 수 있습니다.
     * 
     * @param bookId 도서 ID
     * @param connectedUser 현재 인증된 사용자
     * @return 업데이트된 도서 ID
     * @throws EntityNotFoundException 도서가 존재하지 않을 경우 발생
     * @throws OperationNotPermittedException 현재 사용자가 도서 소유자가 아닌 경우 발생
     */
    public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        
        // 현재 사용자가 도서 소유자인지 확인
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update others books shareable status");
        }
        
        // 공유 가능 상태 반전(토글)
        book.setShareable(!book.isShareable());
        bookRepository.save(book);
        return bookId;
    }

    /**
     * 도서의 보관 상태를 토글합니다.
     * 도서 소유자만 이 작업을 수행할 수 있습니다.
     * 
     * @param bookId 도서 ID
     * @param connectedUser 현재 인증된 사용자
     * @return 업데이트된 도서 ID
     * @throws EntityNotFoundException 도서가 존재하지 않을 경우 발생
     * @throws OperationNotPermittedException 현재 사용자가 도서 소유자가 아닌 경우 발생
     */
    public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        
        // 현재 사용자가 도서 소유자인지 확인
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update others books archived status");
        }
        
        // 보관 상태 반전(토글)
        book.setArchived(!book.isArchived());
        bookRepository.save(book);
        return bookId;
    }

    /**
     * 도서를 대여합니다.
     * 
     * @param bookId 대여할 도서 ID
     * @param connectedUser 현재 인증된 사용자
     * @return 생성된 대여 거래 내역 ID
     * @throws EntityNotFoundException 도서가 존재하지 않을 경우 발생
     * @throws OperationNotPermittedException 다음 경우 발생:
     *         - 도서가 보관 상태이거나 공유 불가능한 경우
     *         - 현재 사용자가 도서 소유자인 경우
     *         - 이미 대여 중인 도서인 경우
     */
    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        
        // 도서가 보관 상태이거나 공유 불가능한 경우 예외 발생
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is archived or not shareable");
        }
        
        User user = ((User) connectedUser.getPrincipal());
        
        // 자신의 도서는 대여할 수 없음
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow your own book");
        }
        
        // 이미 이 사용자가 대여 중인지 확인
        final boolean isAlreadyBorrowedByUser = transactionHistoryRepository.isAlreadyBorrowedByUser(bookId, user.getId());
        if (isAlreadyBorrowedByUser) {
            throw new OperationNotPermittedException("You already borrowed this book and it is still not returned or the return is not approved by the owner");
        }

        // 다른 사용자가 이미 대여 중인지 확인
        final boolean isAlreadyBorrowedByOtherUser = transactionHistoryRepository.isAlreadyBorrowed(bookId);
        if (isAlreadyBorrowedByOtherUser) {
            throw new OperationNotPermittedException("Te requested book is already borrowed");
        }

        // 새 대여 거래 내역 생성
        BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
                .user(user)
                .book(book)
                .returned(false)
                .returnApproved(false)
                .build();
        return transactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    /**
     * 대여한 도서를 반납합니다.
     * 
     * @param bookId 반납할 도서 ID
     * @param connectedUser 현재 인증된 사용자
     * @return 업데이트된 대여 거래 내역 ID
     * @throws EntityNotFoundException 도서가 존재하지 않을 경우 발생
     * @throws OperationNotPermittedException 다음 경우 발생:
     *         - 도서가 보관 상태이거나 공유 불가능한 경우
     *         - 현재 사용자가 도서 소유자인 경우
     *         - 현재 사용자가 이 도서를 대여하지 않은 경우
     */
    public Integer returnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        
        // 도서가 보관 상태이거나 공유 불가능한 경우 예외 발생
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("The requested book is archived or not shareable");
        }
        
        User user = ((User) connectedUser.getPrincipal());
        
        // 자신의 도서는 대여/반납할 수 없음
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow or return your own book");
        }

        // 현재 사용자의 도서 대여 내역 조회
        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("You did not borrow this book"));

        // 반납 상태로 설정
        bookTransactionHistory.setReturned(true);
        return transactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    /**
     * 도서 소유자가 반납된 도서의 반납을 승인합니다.
     * 
     * @param bookId 승인할 도서 ID
     * @param connectedUser 현재 인증된 사용자
     * @return 업데이트된 대여 거래 내역 ID
     * @throws EntityNotFoundException 도서가 존재하지 않을 경우 발생
     * @throws OperationNotPermittedException 다음 경우 발생:
     *         - 도서가 보관 상태이거나 공유 불가능한 경우
     *         - 현재 사용자가 도서 소유자가 아닌 경우
     *         - 도서가 아직 반납되지 않은 경우
     */
    public Integer approveReturnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        
        // 도서가 보관 상태이거나 공유 불가능한 경우 예외 발생
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("The requested book is archived or not shareable");
        }
        
        User user = ((User) connectedUser.getPrincipal());
        
        // 현재 사용자가 도서 소유자인지 확인
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot approve the return of a book you do not own");
        }

        // 도서 소유자 ID로 반납된 도서 대여 내역 조회
        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndOwnerId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("The book is not returned yet. You cannot approve its return"));

        // 반납 승인 상태로 설정
        bookTransactionHistory.setReturnApproved(true);
        return transactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    /**
     * 도서 표지 이미지를 업로드합니다.
     * 
     * @param bookId 도서 ID
     * @param file 업로드할 이미지 파일
     * @param connectedUser 현재 인증된 사용자
     * @throws EntityNotFoundException 도서가 존재하지 않을 경우 발생
     */
    public void uploadBookCoverPicture(Integer bookId, MultipartFile file, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));

        User user = ((User) connectedUser.getPrincipal());

        // 파일 저장 서비스를 통해 이미지 저장
        // 사용자 ID 기반으로 각 사용자별 폴더에 파일 저장
        var bookCover = fileStorageService.saveFile(file, user.getId());
        book.setBookCover(bookCover);
        bookRepository.save(book);
    }
}

/**
 * 고급 구현 및 확장 방법 (시니어 개발자용)
 * 
 * 1. 성능 최적화:
 *    - N+1 문제 해결을 위한 JPQL fetch join 사용
 *    - 자주 조회되는 데이터의 캐싱 전략 구현 (Redis, Caffeine 등)
 *    - 대용량 데이터 처리를 위한 배치 처리 및 페이징 전략 개선
 *    
 *    예시:
 *    @Cacheable(value = "books", key = "#bookId")
 *    public BookResponse findById(Integer bookId) {
 *        // 구현 내용
 *    }
 * 
 * 2. 트랜잭션 관리 강화:
 *    - @Transactional 어노테이션을 사용한 트랜잭션 경계 설정
 *    - 격리 수준 및 전파 속성 최적화
 *    - 낙관적/비관적 락 전략 구현
 *    
 *    예시:
 *    @Transactional(isolation = Isolation.READ_COMMITTED, 
 *                   propagation = Propagation.REQUIRED,
 *                   rollbackFor = Exception.class)
 *    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
 *        // 구현 내용
 *    }
 * 
 * 3. 고급 검색 기능:
 *    - 전문 검색(Full-text search) 구현 (Elasticsearch 통합)
 *    - 필터링, 정렬, 검색을 위한 Specification API 확장
 *    - 동적 쿼리 최적화
 * 
 * 4. 이벤트 기반 아키텍처:
 *    - 도서 대여/반납 시 이벤트 발행 (Spring Events, Kafka 등)
 *    - 비동기 처리로 시스템 응답성 향상
 *    - 관심사 분리를 통한 확장성 증대
 *    
 *    예시:
 *    @Autowired
 *    private ApplicationEventPublisher eventPublisher;
 *    
 *    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
 *        // 기존 로직
 *        BookBorrowedEvent event = new BookBorrowedEvent(this, bookId, user.getId());
 *        eventPublisher.publishEvent(event);
 *        return transactionId;
 *    }
 * 
 * 5. 모니터링 및 감사:
 *    - 중요 비즈니스 활동에 대한 감사 로그 구현
 *    - 성능 매트릭 수집 및 모니터링
 *    - 사용 패턴 분석을 통한 시스템 개선
 * 
 * 6. API 버전 관리:
 *    - 하위 호환성을 위한 API 버전 관리 전략
 *    - DTO 변환 로직 개선 및 MapStruct 활용
 *    - API 문서화 자동화 (SpringDoc, Swagger)
 * 
 * 7. 테스트 강화:
 *    - 단위 테스트와 통합 테스트 커버리지 증대
 *    - 테스트 가능성을 높이기 위한 의존성 주입 패턴 개선
 *    - BDD 스타일 테스트 도입
 * 
 * 8. 도메인 주도 설계(DDD) 적용:
 *    - 풍부한 도메인 모델 구현
 *    - 애그리게이트 및 값 객체 패턴 적용
 *    - 도메인 이벤트를 통한 일관성 유지
 */
