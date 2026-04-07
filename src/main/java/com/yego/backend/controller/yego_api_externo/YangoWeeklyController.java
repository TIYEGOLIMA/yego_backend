package com.yego.backend.controller.yego_api_externo;

import com.yego.backend.entity.yego_api_externo.api.request.YangoSummaryRequest;
import com.yego.backend.entity.yego_api_externo.api.response.YangoSummaryResponse;
import com.yego.backend.exception.YangoWeeklyException;
import com.yego.backend.service.yego_api_externo.YangoWeeklyService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/yango-external")
public class YangoWeeklyController {

    private final YangoWeeklyService yangoWeeklyService;

    public YangoWeeklyController(YangoWeeklyService yangoWeeklyService) {
        this.yangoWeeklyService = yangoWeeklyService;
    }

    @PostMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<YangoSummaryResponse> summary(@RequestBody YangoSummaryRequest request) {
        return ResponseEntity.ok(yangoWeeklyService.summarize(request));
    }

    @ExceptionHandler(YangoWeeklyException.class)
    public ResponseEntity<Map<String, Object>> onYangoWeekly(YangoWeeklyException e) {
        return ResponseEntity.status(e.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(e.toErrorBody());
    }
}
