package com.yego.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class YangoWeeklyException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Object data;

    public YangoWeeklyException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.data = null;
    }

    public YangoWeeklyException(HttpStatus status, String code, String message, Object data) {
        super(message);
        this.status = status;
        this.code = code;
        this.data = data;
    }

    public Map<String, Object> toErrorBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", getMessage());
        if (data != null) {
            body.put("matches", data);
        }
        return body;
    }
}
