package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringRunnerProperties;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.repository.yego_pro_ops.OperationalValidationShiftSampleReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OperationalValidationSampleSelector {

    private final OperationalValidationShiftSampleReadRepository repository;

    @Transactional(readOnly = true)
    public SelectedSample selectSample(OperationalMonitoringRunnerProperties properties) {
        LocalDate effectiveDate = resolveEffectiveDate(properties);
        int limit = properties.sanitizeDefaultDriverCount();
        LocalDateTime from = effectiveDate.atStartOfDay();
        LocalDateTime to = effectiveDate.plusDays(1).atStartOfDay();
        List<ShiftSession> sessions = repository.findForSampleSelection(from, to);

        Set<String> uniqueDriverIds = new LinkedHashSet<>();
        for (ShiftSession session : sessions) {
            if (session.getDriverId() == null || session.getDriverId().isBlank()) {
                continue;
            }
            uniqueDriverIds.add(session.getDriverId().trim());
            if (uniqueDriverIds.size() >= limit) {
                break;
            }
        }

        return new SelectedSample(effectiveDate, List.copyOf(uniqueDriverIds), sessions.size());
    }

    LocalDate resolveEffectiveDate(OperationalMonitoringRunnerProperties properties) {
        if (properties.getDateFrom() != null) {
            return properties.getDateFrom();
        }
        LocalDateTime latestStartedAt = repository.findLatestStartedAt();
        if (latestStartedAt == null) {
            throw new IllegalStateException("No manual shift_sessions data is available for sample selection");
        }
        return latestStartedAt.toLocalDate();
    }

    public record SelectedSample(LocalDate date, List<String> driverIds, int scannedShiftCount) {
    }
}
