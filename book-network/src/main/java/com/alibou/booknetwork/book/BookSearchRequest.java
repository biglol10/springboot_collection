package com.alibou.booknetwork.book;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookSearchRequest {
    private String title;
    private String authorName;
    private String isbn;
    private String synopsis;
    private String bookCover;
    private Boolean archived = null;
    private Boolean shareable = null;
}
