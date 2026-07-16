package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.service.yego_ticketerera.SacStatsExportService;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SacStatsControllerTest {

    @Mock private SacStatsService sacStatsService;
    @Mock private SacStatsExportService sacStatsExportService;

    @Test
    void propagaLaSedeSeleccionadaALasExportaciones() {
        SacStatsController controller = new SacStatsController(sacStatsService, sacStatsExportService);

        controller.exportarAExcel("2026-07-01", "2026-07-16", 10L);
        controller.exportarAImagen("png", "2026-07-01", "2026-07-16", 10L);

        verify(sacStatsExportService).exportarAExcel("2026-07-01", "2026-07-16", 10L);
        verify(sacStatsExportService).exportarAImagen("png", "2026-07-01", "2026-07-16", 10L);
    }
}
