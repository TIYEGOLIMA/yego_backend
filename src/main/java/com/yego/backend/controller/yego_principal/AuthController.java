package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.AuthService;
import com.yego.backend.service.yego_principal.ModuleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ModuleService moduleService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginDto loginDto,
                                                  HttpServletRequest request) {
        LoginResponseDto body = authService.loginResponse(loginDto, request);
        return ResponseEntity.ok()
                .header("X-Access-Token", body.getAccessToken())
                .body(body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(HttpServletRequest request) {
        LoginResponseDto body = authService.refreshFromAuthorizationHeader(
                request.getHeader(HttpHeaders.AUTHORIZATION), request);
        return ResponseEntity.ok()
                .header("X-Access-Token", body.getAccessToken())
                .body(body);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterDto registerDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerDto));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(authService.getUserProfile(userId));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDto changePasswordDto,
                                               Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        authService.changePassword(userId, changePasswordDto.getCurrentPassword(),
                changePasswordDto.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ChangePasswordDto changePasswordDto,
                                              HttpServletRequest request) {
        authService.resetPassword(changePasswordDto, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest request, Authentication authentication) {
        Long userId = (authentication != null && authentication.isAuthenticated())
                ? Long.parseLong(authentication.getName())
                : null;
        authService.cerrarSesionConAuthorizationHeader(userId, request.getHeader(HttpHeaders.AUTHORIZATION));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/force-logout")
    public ResponseEntity<Void> forceLogout(HttpServletRequest request) {
        authService.cerrarSesionConAuthorizationHeader(null, request.getHeader(HttpHeaders.AUTHORIZATION));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-modules")
    public ResponseEntity<List<ModuleResponse>> getMyModules(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(moduleService.obtenerModulosPorUsuario(userId));
    }
}
