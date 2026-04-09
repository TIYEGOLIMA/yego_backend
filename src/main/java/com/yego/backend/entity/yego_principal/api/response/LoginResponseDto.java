package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cuerpo JSON de login/refresh: mensaje, JWT y usuario (mismo shape que {@code GET /auth/profile}).
 * El JWT también va en el header {@code X-Access-Token}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String message;
    private String accessToken;
    private UserProfileDto user;
}
