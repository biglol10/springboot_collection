package com.alibou.booknetwork.exception;

// non-checked exception : 메서드 시그니처에 throws 키워드를 사용하지 않아도 됩니다. 즉, 컴파일 타임에 예외 처리를 확인하지 않습니다. 런타임에 발생하는 예외로 주로 프로그래밍 오류(버그)로 인해 발생합니다.
// Handle this exception in the GlobalExceptionHandler class
public class OperationNotPermittedException extends RuntimeException { // RuntimeException because I want this exception be a non-checked exception
    public OperationNotPermittedException(String msg) {
        super(msg);
    }
}
