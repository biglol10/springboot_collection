package com.alibou.booknetwork.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

// This class is a wrapper for response if we have an exception within our class
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY) // to exclude null fields from the response
public class ExceptionResponse {
    private LocalDateTime timestamp;
    private String path;
    private Integer businessErrorCode;
    private String businessErrorDescription;
    private String error;
    private Set<String> validationErrors;
    private Map<String, String> errors;
}
