package com.yego.backend.controller.yego_api_externo;

import com.yego.backend.entity.yego_api_externo.api.request.YangoSummaryRequest;
import com.yego.backend.entity.yego_api_externo.api.response.YangoSummaryResponse;
import com.yego.backend.entity.yego_api_externo.entities.YangoApiLog;
import com.yego.backend.exception.YangoWeeklyException;
import com.yego.backend.repository.yego_api_externo.YangoApiLogRepository;
import com.yego.backend.service.yego_api_externo.YangoWeeklyService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/yango-external")
public class YangoWeeklyController {

    private final YangoWeeklyService yangoWeeklyService;
    private final YangoApiLogRepository yangoApiLogRepository;

    public YangoWeeklyController(YangoWeeklyService yangoWeeklyService,
                                 YangoApiLogRepository yangoApiLogRepository) {
        this.yangoWeeklyService = yangoWeeklyService;
        this.yangoApiLogRepository = yangoApiLogRepository;
    }

    @PostMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<YangoSummaryResponse> summary(@RequestBody YangoSummaryRequest request) {
        return ResponseEntity.ok(yangoWeeklyService.summarize(request));
    }

    @GetMapping(value = "/logs", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "24") int hours) {

        LocalDateTime desde = LocalDateTime.now().minusHours(hours);

        Long totalCalls = yangoApiLogRepository.countSince(desde);
        List<Object[]> byIp = yangoApiLogRepository.countByIpSince(desde);
        List<YangoApiLog> recent = yangoApiLogRepository.findAllSince(desde);

        List<Map<String, Object>> ipStats = byIp.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ip", row[0]);
            m.put("count", row[1]);
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period_hours", hours);
        result.put("total_calls", totalCalls);
        result.put("calls_by_ip", ipStats);
        result.put("recent_logs", recent);

        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(YangoWeeklyException.class)
    public ResponseEntity<Map<String, Object>> onYangoWeekly(YangoWeeklyException e) {
        return ResponseEntity.status(e.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(e.toErrorBody());
    }
}
