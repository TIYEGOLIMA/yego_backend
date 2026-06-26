package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class OperationalDateRangeParser {

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OperationalMonitoringProperties properties;

    public OperationalDateRangeParser(OperationalMonitoringProperties properties) {
        this.properties = properties;
    }

    public LocalDateTime parseFrom(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parse(raw, true);
    }

    public LocalDateTime parseTo(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parse(raw, false);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(properties.getZoneId());
    }

    private LocalDateTime parse(String raw, boolean startOfDay) {
        try {
            return LocalDateTime.parse(raw, ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        LocalDate date = LocalDate.parse(raw, ISO_LOCAL_DATE);
        return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
    }
}
