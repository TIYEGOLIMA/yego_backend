package com.yego.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Servicio para manejar la subida de archivos a MinIO
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Service
@Slf4j
public class MinIOService {

    private final RestTemplate restTemplate;
    
    @Value("${minio.url:http://178.156.204.129:3000/media}")
    private String minioUrl;
    
    @Value("${minio.bucket:yego-integral}")
    private String bucketName;

    public MinIOService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Sube un archivo a MinIO
     * 
     * @param file Archivo a subir
     * @return URL del archivo subido o null si hay error
     */
    public String subirArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("⚠️ [MinIOService] Intento de subir archivo vacío o nulo");
            return null;
        }

        try {
            log.info("📤 [MinIOService] Subiendo archivo: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

            // Preparar los headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Preparar el body con form-data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("bucket", bucketName);
            body.add("file", file.getResource());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Realizar la petición POST
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    minioUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String url = (String) responseBody.get("url");
                Integer status = (Integer) responseBody.get("status");
                
                if (url != null && status != null && status == 200) {
                    log.info("✅ [MinIOService] Archivo subido exitosamente: {}", url);
                    return url;
                } else {
                    log.error("❌ [MinIOService] Respuesta inesperada de MinIO: {}", responseBody);
                    return null;
                }
            } else {
                log.error("❌ [MinIOService] Error en la respuesta de MinIO. Status: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("❌ [MinIOService] Error al subir archivo a MinIO: {}", e.getMessage(), e);
            return null;
        }
    }
}

