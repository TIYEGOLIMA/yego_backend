package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.AdminDriverHistoryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.AdminDriverResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.repository.yego_api_externo.DriverApiRepository;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MobileAdminDriverService {

    private static final String YEGO_PRO_OPS_PARK_ID = "64085dd85e124e2c808806f70d527ea8";
    private static final int SEARCH_RESULT_LIMIT = 30;
    private static final int MAX_SEARCH_TERM_LENGTH = 80;
    private static final int DEFAULT_HISTORY_PAGE_SIZE = 20;
    private static final int MAX_HISTORY_PAGE_SIZE = 50;

    private final DriverApiRepository driverRepository;
    private final ShiftSessionRepository shiftRepository;
    private final DriverCloseRepository closeRepository;
    private final MobileShiftResponseMapper responseMapper;

    public List<AdminDriverResponse> search(String query) {
        String term = query == null ? "" : query.trim();
        if (term.isEmpty()) {
            return Collections.emptyList();
        }
        if (term.length() > MAX_SEARCH_TERM_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Búsqueda demasiado larga");
        }

        return driverRepository.searchByPark(
                        YEGO_PRO_OPS_PARK_ID,
                        term,
                        PageRequest.of(0, SEARCH_RESULT_LIMIT)
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    public AdminDriverHistoryResponse getHistory(String driverId, int page, int size) {
        DriverApi driver = findYegoProOpsDriver(driverId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_HISTORY_PAGE_SIZE : Math.min(size, MAX_HISTORY_PAGE_SIZE);
        Page<ShiftSession> shifts = shiftRepository.findByDriverIdAndDeletedFalseOrderByStartedAtDescIdDesc(
                driverId,
                PageRequest.of(safePage, safeSize)
        );
        Map<UUID, DriverClose> closesBySession = findLatestCloses(shifts.getContent());

        return AdminDriverHistoryResponse.builder()
                .driver(toResponse(driver))
                .shifts(shifts.getContent().stream()
                        .map(session -> responseMapper.toResponse(
                                session,
                                closesBySession.get(session.getId()),
                                null
                        ))
                        .toList())
                .page(shifts.getNumber())
                .size(shifts.getSize())
                .totalElements(shifts.getTotalElements())
                .totalPages(shifts.getTotalPages())
                .hasMore(shifts.hasNext())
                .build();
    }

    public MobileShiftResponse getShiftDetail(String sessionId) {
        ShiftSession session = findShift(sessionId);
        findYegoProOpsDriver(session.getDriverId());
        DriverClose close = closeRepository.findFirstByShiftSessionIdOrderByIdDesc(session.getId()).orElse(null);
        return responseMapper.toResponse(session, close, null);
    }

    private ShiftSession findShift(String sessionId) {
        try {
            return shiftRepository.findById(UUID.fromString(sessionId))
                    .filter(session -> !Boolean.TRUE.equals(session.getDeleted()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado");
        }
    }

    private Map<UUID, DriverClose> findLatestCloses(List<ShiftSession> sessions) {
        if (sessions.isEmpty()) return Collections.emptyMap();

        return closeRepository.findByShiftSessionIdIn(sessions.stream().map(ShiftSession::getId).toList())
                .stream()
                .filter(close -> close.getShiftSessionId() != null)
                .collect(Collectors.toMap(
                        DriverClose::getShiftSessionId,
                        close -> close,
                        (left, right) -> left.getId() >= right.getId() ? left : right
                ));
    }

    private DriverApi findYegoProOpsDriver(String driverId) {
        return driverRepository.findById(driverId)
                .filter(driver -> YEGO_PRO_OPS_PARK_ID.equals(driver.getParkId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conductor Yego Pro Ops no encontrado"
                ));
    }

    private AdminDriverResponse toResponse(DriverApi driver) {
        String licenseNumber = firstNotBlank(driver.getLicenseNumber(), driver.getLicenseNormalizedNumber());
        return AdminDriverResponse.builder()
                .driverId(driver.getDriverId())
                .fullName(resolveFullName(driver))
                .licenseNumber(licenseNumber)
                .documentType(driver.getDocumentType())
                .documentNumber(firstNotBlank(driver.getDocumentNumber(), deriveDni(licenseNumber)))
                .phone(driver.getPhone())
                .workStatus(driver.getWorkStatus())
                .active(driver.getActive())
                .carNumber(driver.getCarNumber())
                .build();
    }

    private String deriveDni(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.isBlank()) return null;
        String normalized = licenseNumber.trim();
        return Character.isLetter(normalized.charAt(0)) ? normalized.substring(1) : normalized;
    }

    private String resolveFullName(DriverApi driver) {
        String fullName = firstNotBlank(driver.getFullName());
        if (fullName != null) {
            return fullName;
        }

        String composed = Stream.of(driver.getFirstName(), driver.getMiddleName(), driver.getLastName())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElse(null);
        return composed != null ? composed : "Conductor sin nombre";
    }

    private String firstNotBlank(String... values) {
        return Stream.of(values)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }
}
