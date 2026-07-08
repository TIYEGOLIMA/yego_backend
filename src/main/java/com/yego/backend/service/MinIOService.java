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
        return subirArchivo(file, bucketName, null);
    }

    /**
     * Sube un archivo a un bucket específico con un nombre de objeto controlado.
     *
     * @param file       Archivo a subir
     * @param bucket     Bucket destino
     * @param objectName Nombre/ruta del objeto (ej. "ABC123/SOAT-1.pdf"). Si es null usa el original.
     * @return URL del archivo subido o null si hay error
     */
    public String subirArchivo(MultipartFile file, String bucket, String objectName) {
        if (file == null || file.isEmpty()) {
            log.warn("⚠️ [MinIOService] Intento de subir archivo vacío o nulo");
            return null;
        }

        try {
            final String targetBucket = (bucket != null && !bucket.isBlank()) ? bucket : bucketName;
            final String nombreFinal = (objectName != null && !objectName.isBlank())
                    ? objectName
                    : file.getOriginalFilename();

            log.info("📤 [MinIOService] Subiendo archivo '{}' a bucket '{}' ({} bytes)", nombreFinal, targetBucket, file.getSize());

            // Preparar los headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Recurso con el nombre de objeto deseado (el gateway usa el filename del recurso).
            org.springframework.core.io.Resource resource;
            try {
                resource = new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return nombreFinal;
                    }
                };
            } catch (Exception ex) {
                resource = file.getResource();
            }

            // Preparar el body con form-data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("bucket", targetBucket);
            body.add("file", resource);

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

    /**
     * Elimina un archivo de MinIO basado en su URL
     * 
     * @param fileUrl URL del archivo a eliminar
     * @return true si se eliminó exitosamente, false en caso contrario
     */
    public boolean eliminarArchivo(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            log.warn("⚠️ [MinIOService] Intento de eliminar archivo con URL vacía o nula");
            return false;
        }

        try {
            log.info("🗑️ [MinIOService] Eliminando archivo: {}", fileUrl);

            // Extraer el nombre del archivo de la URL
            // Ejemplo: https://s3.yego.pro/yego-integral/archivo.pdf -> archivo.pdf
            String fileName = extraerNombreArchivoDeUrl(fileUrl);
            if (fileName == null || fileName.isEmpty()) {
                log.warn("⚠️ [MinIOService] No se pudo extraer el nombre del archivo de la URL: {}", fileUrl);
                return false;
            }

            // Preparar los headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Preparar el body
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("bucket", bucketName);
            body.add("fileName", fileName);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // Realizar la petición DELETE (o POST según la API de MinIO)
            // Nota: Ajustar según la API real de MinIO
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    minioUrl + "/delete",  // Ajustar endpoint según la API
                    HttpMethod.POST,  // O DELETE según la API
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [MinIOService] Archivo eliminado exitosamente: {}", fileName);
                return true;
            } else {
                log.error("❌ [MinIOService] Error eliminando archivo. Status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("❌ [MinIOService] Error al eliminar archivo de MinIO: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extrae el nombre del archivo de una URL
     */
    private String extraerNombreArchivoDeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            // Buscar la última parte después de la última barra
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                String fileName = url.substring(lastSlash + 1);
                // Eliminar parámetros de consulta si existen
                int queryIndex = fileName.indexOf('?');
                if (queryIndex > 0) {
                    fileName = fileName.substring(0, queryIndex);
                }
                return fileName;
            }
        } catch (Exception e) {
            log.warn("⚠️ [MinIOService] Error extrayendo nombre de archivo de URL: {}", e.getMessage());
        }
        
        return null;
    }
}

