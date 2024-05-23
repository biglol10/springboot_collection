package com.alibou.booknetwork.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED) // what will be the response status by default
    public ResponseEntity<?> register(
            @RequestBody @Valid RegisterationRequest registerationRequest
    ) {
        authenticationService.register(registerationRequest);
        return ResponseEntity.accepted().build();
    }
}
