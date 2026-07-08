package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.service.MinIOService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MobileFileService {

    private static final String BUCKET_TURNOS = "yego-pro-ops-turnos";
    private static final String BUCKET_GASTOS = "yego-pro-ops-gastos";
    private static final String BUCKET_SELFIE = "yego-pro-ops-selfie";
    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");

    private final MinIOService minIOService;

    public String uploadShiftImage(
            MultipartFile file,
            String type,
            String driverId,
            String sessionId,
            String placa,
            String driverName
    ) {
        String url = minIOService.subirArchivo(
                file,
                resolveBucket(type),
                buildObjectName(file.getOriginalFilename(), type, driverId, sessionId, placa, driverName)
        );

        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo subir la imagen a MinIO");
        }

        return url;
    }

    private String resolveBucket(String type) {
        String normalized = sanitize(type);
        if (normalized.startsWith("gasto-")) {
            return BUCKET_GASTOS;
        }
        if (normalized.contains("selfie")) {
            return BUCKET_SELFIE;
        }
        return BUCKET_TURNOS;
    }

    private String buildObjectName(
            String filename,
            String type,
            String driverId,
            String sessionId,
            String placa,
            String driverName
    ) {
        LocalDateTime now = LocalDateTime.now(LIMA_ZONE);
        return now.toLocalDate().format(DAY_FORMAT)
                + "/" + sanitize(placa)
                + "/" + sanitize(driverName)
                + "/" + sanitize(driverId)
                + "/" + sanitize(sessionId)
                + "/" + sanitize(type)
                + "-" + now.format(TIME_FORMAT)
                + "-" + UUID.randomUUID()
                + extension(filename);
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return "sin-dato";
        return value.replaceAll("[^A-Za-z0-9_-]", "-");
    }

    private String extension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return ".jpg";
        String ext = filename.substring(dot).toLowerCase();
        return ext.matches("\\.(jpg|jpeg|png|webp|heic|heif)") ? ext : ".jpg";
    }
}
