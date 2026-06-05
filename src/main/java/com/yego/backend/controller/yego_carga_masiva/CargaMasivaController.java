package com.yego.backend.controller.yego_carga_masiva;

import com.yego.backend.entity.yego_carga_masiva.api.response.CargaHistorialResponse;
import com.yego.backend.entity.yego_carga_masiva.api.response.CargaMasivaImportResponse;
import com.yego.backend.entity.yego_carga_masiva.api.response.CargaMasivaPreviewResponse;
import com.yego.backend.service.yego_carga_masiva.CargaMasivaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/carga-masiva")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CargaMasivaController {

    private final CargaMasivaService cargaMasivaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CargaMasivaPreviewResponse uploadExcel(@RequestParam("file") MultipartFile file) {
        log.info("[CargaMasiva] Archivo recibido: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
        return cargaMasivaService.previewExcel(file);
    }

    @PostMapping("/import")
    public CargaMasivaImportResponse importExcel(@RequestBody Map<String, String> body) {
        String cargaId = body.get("cargaId");
        log.info("[CargaMasiva] Importando cargaId={}", cargaId);
        return cargaMasivaService.importExcel(cargaId);
    }

    @GetMapping("/historial")
    public List<CargaHistorialResponse> getHistorial() {
        return cargaMasivaService.getHistorial();
    }
}
