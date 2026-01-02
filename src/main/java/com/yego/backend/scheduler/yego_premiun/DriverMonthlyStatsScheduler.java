package com.yego.backend.scheduler.yego_premiun;

import com.yego.backend.handler.yego_premiun.DriverMonthlyStatsNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.YearMonth;

@Component
@Slf4j
@RequiredArgsConstructor
public class DriverMonthlyStatsScheduler {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    private final DriverMonthlyStatsNotificationHandler notificationHandler;


    /**
     * Notifica al frontend el primer día de cada mes a las 08:00 AM (hora Lima)
     * que ya puede procesar el periodo anterior.
     */
    @Scheduled(cron = "0 0 8 1 * *", zone = "America/Lima")
    public void notifyProcessingWindow() {
        YearMonth periodo = YearMonth.now(LIMA_ZONE).minusMonths(1);
        log.info("🗓️ [DriverMonthlyStatsScheduler] Notificando disponibilidad de procesamiento para {}-{}", periodo.getYear(), periodo.getMonthValue());
        notificationHandler.notifyProcessAvailable(periodo);
    }
}

