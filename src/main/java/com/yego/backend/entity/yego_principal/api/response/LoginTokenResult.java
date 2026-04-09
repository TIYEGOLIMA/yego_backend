package com.yego.backend.entity.yego_principal.api.response;

/**
 * Resultado de login/refresh: token + mensaje + perfil. El token también se envía en {@code X-Access-Token}.
 */
public record LoginTokenResult(String accessToken, String message, UserProfileDto user) {}
