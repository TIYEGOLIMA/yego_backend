package com.yego.backend.controller;

import com.yego.backend.service.yego_principal.WebSocketAccessTicketService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/ws")
@RequiredArgsConstructor
public class WebSocketAccessTicketController {

    private final WebSocketAccessTicketService accessTicketService;

    @PostMapping("/ticket")
    public ResponseEntity<Map<String, Object>> issue(HttpServletRequest request) {
        Object attribute = request.getAttribute("jwtClaims");
        if (!(attribute instanceof Claims claims)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión autenticada requerida");
        }
        var issued = accessTicketService.issue(claims);
        return ResponseEntity.ok(Map.of(
                "ticket", issued.ticket(),
                "expiresAt", issued.expiresAt().toString()
        ));
    }
}

