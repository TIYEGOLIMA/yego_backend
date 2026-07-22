package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SacStatsExportServiceImplTest {

    @Mock private SacStatsService sacStatsService;

    @Test
    void exportaUnaHojaDeTrazabilidadConSedeYOpcionMarcada() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 16, 10, 0);
        SacStatsResponse.TicketTraceabilityResponse trace =
                SacStatsResponse.TicketTraceabilityResponse.builder()
                        .id(101L)
                        .ticketNumber("M-101")
                        .status("COMPLETED")
                        .sedeName("Sede Lima")
                        .categoryName("Cuenta del conductor")
                        .optionId(12L)
                        .optionName("Actualización de datos")
                        .licenseNumber("+51999999999")
                        .operatorName("María Operadora")
                        .moduleName("Módulo 4")
                        .createdAt(createdAt)
                        .completedAt(createdAt.plusMinutes(5))
                        .rating(5)
                        .events(List.of(SacStatsResponse.TicketTraceEventResponse.builder()
                                .status("GENERATED")
                                .label("Ticket generado")
                                .occurredAt(createdAt)
                                .build()))
                        .build();
        SacStatsResponse stats = SacStatsResponse.builder()
                .totalTickets(1)
                .openTickets(0)
                .completedTickets(1)
                .cancelledTickets(0)
                .averageRating(5.0)
                .totalRatings(1)
                .sacPerformance(List.of())
                .hourlyDistribution(List.of())
                .hourlyBySede(List.of())
                .optionSelectionsBySede(List.of(
                        SacStatsResponse.OptionSelectionBySedeResponse.builder()
                                .sedeId(10L)
                                .sedeName("Sede Lima")
                                .totalTickets(1)
                                .options(List.of(SacStatsResponse.OptionSelectionResponse.builder()
                                        .optionId(12L)
                                        .categoryName("Cuenta del conductor")
                                        .optionName("Actualización de datos")
                                        .count(1)
                                        .percentage(100.0)
                                        .build()))
                                .build()))
                .ticketTraceability(List.of(trace))
                .build();
        when(sacStatsService.obtenerTodasLasEstadisticas("2026-07-16", "2026-07-16", 10L))
                .thenReturn(stats);

        SacStatsExportServiceImpl service = new SacStatsExportServiceImpl(sacStatsService);
        ResponseEntity<byte[]> response = service.exportarAExcel("2026-07-16", "2026-07-16", 10L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getBody()))) {
            assertThat(workbook.getSheet("Trazabilidad")).isNotNull();
            assertThat(workbook.getSheet("Trazabilidad").getRow(0).getCell(4).getStringCellValue())
                    .isEqualTo("Opción marcada");
            assertThat(workbook.getSheet("Trazabilidad").getRow(1).getCell(2).getStringCellValue())
                    .isEqualTo("Sede Lima");
            assertThat(workbook.getSheet("Trazabilidad").getRow(1).getCell(4).getStringCellValue())
                    .isEqualTo("Actualización de datos");
            assertThat(workbook.getSheet("Trazabilidad").getRow(1).getCell(13).getStringCellValue())
                    .contains("Ticket generado");
            assertThat(workbook.getSheet("Opciones por sede")).isNotNull();
            assertThat(workbook.getSheet("Opciones por sede").getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("Sede Lima");
            assertThat(workbook.getSheet("Opciones por sede").getRow(1).getCell(2).getStringCellValue())
                    .isEqualTo("Actualización de datos");
        }

        ResponseEntity<byte[]> imageResponse = service.exportarAImagen(
                "png", "2026-07-16", "2026-07-16", 10L);
        assertThat(imageResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(imageResponse.getBody()).isNotNull();
        var image = ImageIO.read(new ByteArrayInputStream(imageResponse.getBody()));
        assertThat(image.getWidth()).isEqualTo(2400);
        assertThat(image.getHeight()).isGreaterThan(600);
    }
}
