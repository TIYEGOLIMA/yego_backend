package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.mobile.MobileOtpRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.mobile.MobileOtpVerifyRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileAuthResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileOtpResponse;
import com.yego.backend.service.yego_pro_ops.mobile.MobileAuthService;
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

    @PostMapping("/request-otp")
    public ResponseEntity<MobileOtpResponse> requestOtp(@Valid @RequestBody MobileOtpRequest request) {
        log.info("Solicitud OTP movil: licencia={}", request.getLicenseNumber());
        return ResponseEntity.ok(service.requestOtp(request.getLicenseNumber()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<MobileAuthResponse> verifyOtp(@Valid @RequestBody MobileOtpVerifyRequest request) {
        return ResponseEntity.ok(service.verifyOtp(request.getLicenseNumber(), request.getCode()));
    }
}
