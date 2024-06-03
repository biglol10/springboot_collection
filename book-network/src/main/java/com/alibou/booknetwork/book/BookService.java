package com.alibou.booknetwork.book;

import com.alibou.booknetwork.common.PageResponse;
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

import java.util.List;

import static com.alibou.booknetwork.book.BookSpecification.withOwnerId;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final BookTransactionHistoryRepository transactionHistoryRepository;

    public Integer save(BookRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal(); // getPrincipal gets the current user and since User implements UserDetails and Principal, we can be sure that getPrincipal can be casted to User
        // Principal is an interface that represents the identity of a user. It typically contains the user's name, but it can also contain additional information such as the user's email address, user ID, etc.
        // UserDetails is an interface that represents a user's details. It typically contains the user's username, password, and authorities (roles). UserDetails is used by Spring Security to authenticate and authorize users.
        // In Spring Security, the Principal object is typically an instance of UserDetails. This allows Spring Security to access the user's details (username, password, authorities) when processing authentication and authorization requests.
        // When a user is authenticated, Spring Security creates an instance of UserDetails (e.g., User) and stores it in the SecurityContext. This UserDetails object is then accessible via the Authentication object, which can be obtained from the SecurityContext.
        // We can customize getPrincipal by implementing our own UserDetails class and overriding the getPrincipal method to return our custom UserDetails object.

        Book book = bookMapper.toBook(request);
        book.setOwner(user);
        return bookRepository.save(book).getId();
    }

    public BookResponse findById(Integer bookId) {
        return bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, user.getId()); // display sharable books that are not archived and not owned by the connected user
        List<BookResponse> bookResponses = books.map(bookMapper::toBookResponse).toList();

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


    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Book> books = bookRepository.findAll(withOwnerId(user.getId()), pageable); // to support specification, we need to add JpaSpecificationExecutor<Book> to the BookRepository interface

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

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
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
}
