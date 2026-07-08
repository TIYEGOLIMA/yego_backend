package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.CloseShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.OpenShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MobileShiftSummaryResponse;
import com.yego.backend.service.yego_pro_ops.MobileShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/mobile/shifts")
@RequiredArgsConstructor
public class MobileShiftController {

    private final MobileShiftService mobileShiftService;

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public MobileShiftResponse openShift(@Valid @RequestBody OpenShiftMobileRequest request) {
        return mobileShiftService.openShift(request);
    }

    @PostMapping("/{sessionId}/close")
    public MobileShiftResponse closeShift(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CloseShiftMobileRequest request) {
        return mobileShiftService.closeShift(sessionId, request);
    }

    @DeleteMapping("/{sessionId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelShift(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String reason) {
        mobileShiftService.cancelShift(sessionId, userId, reason);
    }

    @GetMapping("/active/{driverId}")
    public MobileShiftResponse getActiveShift(@PathVariable String driverId) {
        return mobileShiftService.getActiveShift(driverId);
    }

    @GetMapping("/driver/{driverId}")
    public List<MobileShiftResponse> getDriverHistory(@PathVariable String driverId) {
        return mobileShiftService.getDriverHistory(driverId);
    }

    @GetMapping("/{sessionId}/summary")
    public MobileShiftSummaryResponse getSummary(@PathVariable UUID sessionId) {
        return mobileShiftService.getSummary(sessionId);
    }
}
