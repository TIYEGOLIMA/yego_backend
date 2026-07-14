package com.yego.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;

import org.apache.catalina.connector.ClientAbortException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Respuestas JSON uniformes: {@code message} y opcionalmente {@code code}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalApiExceptionHandler {

    private static Map<String, Object> errorBody(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("code", code);
        return body;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String msg = ex.getReason();
        if (msg == null || msg.isBlank()) {
            msg = "Error en la solicitud";
        }
        String code = status.name();
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody(code, msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(m -> m != null && !m.isBlank())
                .collect(Collectors.joining("; "));
        if (detail.isBlank()) {
            detail = ex.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": inválido")
                    .collect(Collectors.joining("; "));
        }
        if (detail.isBlank()) {
            detail = "Los datos enviados no son válidos";
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody("VALIDATION_ERROR", detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody("BAD_REQUEST", "Cuerpo JSON inválido o incompleto"));
    }

    /**
     * Cliente cerró la conexión (timeout, navegación, proxy) mientras el servidor enviaba el cuerpo.
     * Típico al serializar listas grandes; no indica fallo interno del servicio.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncBrokenPipe(AsyncRequestNotUsableException ex) {
        Throwable deepest = deepestCause(ex);
        if (deepest instanceof java.io.IOException) {
            log.debug("Cliente desconectó durante la respuesta ({})", shorten(deepest.getMessage()));
            return;
        }
        log.debug("Petición asíncrona no usable ({})", shorten(ex.getMessage()));
    }

    /** Tomcat: mismo escenario cuando el navegador cancela antes de tiempo. */
    @ExceptionHandler(ClientAbortException.class)
    public void handleTomcatClientAbort(ClientAbortException ex) {
        log.debug("Cliente cerró la conexión durante la respuesta ({})", shorten(ex.getMessage()));
    }

    private static Throwable deepestCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    private static String shorten(String s) {
        if (s == null) return "";
        return s.length() > 120 ? s.substring(0, 117) + "..." : s;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody("INTERNAL_ERROR", "Error interno. Intenta de nuevo o contacta soporte."));
    }
}
