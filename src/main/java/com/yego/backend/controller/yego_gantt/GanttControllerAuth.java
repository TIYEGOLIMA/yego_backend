package com.yego.backend.controller.yego_gantt;

import org.springframework.security.core.Authentication;

/** ID de usuario de la sesión Spring Security (principal = string numérico). */
final class GanttControllerAuth {

    private GanttControllerAuth() {}

    static long userId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
