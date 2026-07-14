package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.mobile.AdminDashboardResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.ShiftLocationResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.ShiftRouteResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.VehiclePhotoContent;
import com.yego.backend.service.yego_pro_ops.VehicleService;
import com.yego.backend.service.yego_pro_ops.mobile.MobileShiftLocationService;
import com.yego.backend.service.yego_pro_ops.mobile.MobileShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/mobile/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MobileAdminController {

    private final VehicleService vehicleService;
    private final MobileShiftLocationService locationService;
    private final MobileShiftService shiftService;

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return vehicleService.obtenerDashboardAdmin();
    }

    @GetMapping("/locations/active")
    public List<ShiftLocationResponse> activeLocations() {
        return locationService.getActiveLocations();
    }

    @GetMapping("/shifts/{sessionId}/route")
    public ShiftRouteResponse shiftRoute(@PathVariable String sessionId) {
        return locationService.getRoute(sessionId);
    }

    @GetMapping("/shifts/{sessionId}/summary")
    public MobileShiftSummaryResponse shiftSummary(@PathVariable String sessionId) {
        return shiftService.getSummary(sessionId);
    }

    @GetMapping("/vehicles/{yangoCarId}/photo")
    public ResponseEntity<byte[]> vehiclePhoto(@PathVariable String yangoCarId) {
        try {
            VehiclePhotoContent photo = vehicleService.obtenerFotoVehiculoMobile(yangoCarId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(photo.contentType()))
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                    .body(photo.content());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
