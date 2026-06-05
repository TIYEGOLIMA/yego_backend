package com.yego.backend.service.yego_carga_masiva;

import com.yego.backend.entity.yego_carga_masiva.api.response.CargaHistorialResponse;
import com.yego.backend.entity.yego_carga_masiva.api.response.CargaMasivaImportResponse;
import com.yego.backend.entity.yego_carga_masiva.api.response.CargaMasivaPreviewResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CargaMasivaService {

    CargaMasivaPreviewResponse previewExcel(MultipartFile file);

    CargaMasivaImportResponse importExcel(String cargaId);

    List<CargaHistorialResponse> getHistorial();
}
