package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.mobile.MobileOtpRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.mobile.MobileOtpVerifyRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileAuthResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileOtpResponse;
import com.yego.backend.service.yego_pro_ops.mobile.MobileAuthService;
import com.yego.backend.service.yego_pro_ops.mobile.MobileDriverAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MobileAuthController {

    private final MobileAuthService service;
    private final MobileDriverAuthService mobileDriverAuthService;

    @PostMapping("/request-otp")
    public ResponseEntity<MobileOtpResponse> requestOtp(
            @Valid @RequestBody MobileOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Solicitud OTP movil: licencia={}", request.getLicenseNumber());
        return ResponseEntity.ok(service.requestOtp(
                request.getLicenseNumber(),
                request.getDeviceId(),
                request.getAppVersion(),
                resolveClientIp(httpRequest)
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<MobileAuthResponse> verifyOtp(@Valid @RequestBody MobileOtpVerifyRequest request) {
        return ResponseEntity.ok(service.verifyOtp(
                request.getLicenseNumber(),
                request.getCode(),
                request.getDeviceId(),
                request.getAppVersion()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        MobileDriverAuthService.MobileDriverIdentity identity = mobileDriverAuthService.requireIdentity(request);
        service.logout(identity.driverId(), identity.mobileSessionId());
        return ResponseEntity.noContent().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
