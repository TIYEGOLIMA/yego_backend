package com.yego.backend.entity.yego_principal.api.response;

/**
 * Resultado interno de login/refresh: el token se envía al cliente en el header {@code X-Access-Token},
 * no en el cuerpo JSON.
 */
public record LoginTokenResult(String accessToken, String message) {}
