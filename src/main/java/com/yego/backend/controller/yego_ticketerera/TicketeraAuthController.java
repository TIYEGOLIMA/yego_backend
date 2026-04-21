package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_principal.api.response.LoginResponseDto;
import com.yego.backend.service.yego_principal.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ticketera/auth")
@RequiredArgsConstructor
public class TicketeraAuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(HttpServletRequest request) {
        LoginResponseDto body = authService.refreshFromAuthorizationHeader(
                request.getHeader(HttpHeaders.AUTHORIZATION), request);
        return ResponseEntity.ok()
                .header("X-Access-Token", body.getAccessToken())
                .body(body);
    }
}
