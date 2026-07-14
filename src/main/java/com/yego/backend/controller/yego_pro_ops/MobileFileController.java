package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.service.yego_pro_ops.mobile.MobileFileService;
import com.yego.backend.service.yego_pro_ops.mobile.MobileDriverAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/mobile/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MobileFileController {

    private final MobileFileService mobileFileService;
    private final MobileDriverAuthService mobileDriverAuthService;

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "turno") String type,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String placa,
            @RequestParam(required = false) String driverName,
            HttpServletRequest request
    ) {
        String driverId = mobileDriverAuthService.requireDriverId(request);
        String url = mobileFileService.uploadShiftImage(file, type, driverId, sessionId, placa, driverName);
        return Map.of("url", url);
    }
}
