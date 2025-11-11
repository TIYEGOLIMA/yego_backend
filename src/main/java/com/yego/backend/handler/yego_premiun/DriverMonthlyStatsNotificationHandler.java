package com.yego.backend.handler.yego_premiun;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class DriverMonthlyStatsNotificationHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyProcessAvailable(YearMonth period) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", "PREMIUN_PROCESS_AVAILABLE",
                    "message", String.format("Procesa las estadísticas del periodo %s %d", period.getMonth(), period.getYear()),
                    "year", period.getYear(),
                    "month", period.getMonthValue(),
                    "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
            );

            messagingTemplate.convertAndSend("/topic/yego-premiun", payload);
            messagingTemplate.convertAndSend("/topic/premium-driver", payload);

            log.info("📢 [DriverMonthlyStatsNotification] Notificación enviada: {}", payload);
        } catch (Exception ex) {
            log.error("❌ [DriverMonthlyStatsNotification] Error enviando notificación: {}", ex.getMessage());
        }
    }
}


