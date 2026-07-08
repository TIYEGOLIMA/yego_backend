package com.yego.backend.config.yego_pro_ops;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@ConfigurationProperties(prefix = "operational.monitoring.runner")
@Getter
@Setter
public class OperationalMonitoringRunnerProperties {

    private boolean enabled = false;
    private String environment = "unknown";
    private boolean confirmWritesToOperationalTables = false;
    private int maxRangeDays = 1;
    private int defaultDriverCount = 5;
    private int maxDriverCount = 20;
    private boolean useManualSampleSelector = true;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private List<String> driverIds = new ArrayList<>();
    private String vehicleKey;

    public String normalizedEnvironment() {
        return environment == null ? "unknown" : environment.trim().toLowerCase(Locale.ROOT);
    }

    public List<String> normalizedDriverIds() {
        return driverIds == null
                ? List.of()
                : driverIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public int sanitizeDefaultDriverCount() {
        if (defaultDriverCount <= 0) {
            return 5;
        }
        return Math.min(defaultDriverCount, sanitizeMaxDriverCount());
    }

    public int sanitizeMaxDriverCount() {
        if (maxDriverCount <= 0) {
            return 20;
        }
        return Math.min(maxDriverCount, 20);
    }
}
