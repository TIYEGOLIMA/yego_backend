package com.yego.backend.service.yego_api_externo;

import com.yego.backend.entity.yego_api_externo.api.request.YangoSummaryRequest;
import com.yego.backend.exception.YangoWeeklyException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangoWeeklyServiceTest {

    @Test
    void resolveQueryText_withText() {
        YangoSummaryRequest r = YangoSummaryRequest.builder().text("Q72795009").build();
        assertEquals("Q72795009", YangoWeeklyService.resolveQueryText(r));
    }

    @Test
    void resolveQueryText_blankThrows() {
        YangoSummaryRequest r = YangoSummaryRequest.builder().text("   ").build();
        YangoWeeklyException ex =
                assertThrows(YangoWeeklyException.class, () -> YangoWeeklyService.resolveQueryText(r));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void resolveWeeklyPeriod_mondayToSunday() {
        LocalDate anchor = LocalDate.of(2026, 4, 8);
        YangoWeeklyService.PeriodRange p = YangoWeeklyService.resolveWeeklyPeriod(anchor);
        assertTrue(p.dateFrom().contains("2026-04-06T00:00:00"));
        assertTrue(p.dateTo().contains("2026-04-12T23:59:59"));
    }

    @Test
    void resolveLastMonthPeriod_previousMonth() {
        LocalDate anchor = LocalDate.of(2026, 4, 15);
        YangoWeeklyService.PeriodRange p = YangoWeeklyService.resolveLastMonthPeriod(anchor);
        assertTrue(p.dateFrom().contains("2026-03-01T00:00:00"));
        assertTrue(p.dateTo().contains("2026-03-31T23:59:59"));
    }

    @Test
    void resolveLastMonthPeriod_january_getsDecember() {
        LocalDate anchor = LocalDate.of(2026, 1, 10);
        YangoWeeklyService.PeriodRange p = YangoWeeklyService.resolveLastMonthPeriod(anchor);
        assertTrue(p.dateFrom().contains("2025-12-01T00:00:00"));
        assertTrue(p.dateTo().contains("2025-12-31T23:59:59"));
    }
}
