package com.yego.backend.service.yego_carga_masiva.impl;

import com.yego.backend.entity.yego_carga_masiva.api.response.CargaHistorialResponse;
import com.yego.backend.entity.yego_carga_masiva.api.response.CargaMasivaImportResponse;
import com.yego.backend.entity.yego_carga_masiva.api.response.CargaMasivaPreviewResponse;
import com.yego.backend.service.yego_carga_masiva.CargaMasivaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CargaMasivaServiceImpl implements CargaMasivaService {

    private static final DateTimeFormatter[] FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    };

    private static final String[] EXCEL_HEADERS = {
        "Upload date", "Payout ID", "Tracking Code", "Order ID",
        "Beneficiary name", "Beneficiary last name", "Beneficiary document type",
        "Beneficiary document number", "Amount", "Paid amount", "Currency",
        "Status", "Destination Bank Name", "Destination bank account number",
        "Account Type", "Output Stage", "Error message"
    };

    private static final String INSERT_SQL = "INSERT INTO module_ct_excel_payouts (" +
        "upload_date, payout_id, tracking_code, order_id, " +
        "beneficiary_name, beneficiary_last_name, beneficiary_doc_type, beneficiary_doc_number, " +
        "amount, paid_amount, currency, status, bank_name, bank_account, account_type, " +
        "output_stage, error_message" +
        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (payout_id) DO NOTHING";

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, List<List<String>>> cache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> bloqueados = new ConcurrentHashMap<>();

    public CargaMasivaServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CargaMasivaPreviewResponse previewExcel(MultipartFile file) {
        try {
            String cargaId = UUID.randomUUID().toString();
            List<String> excelHeaders = new ArrayList<>();
            List<List<String>> allRows = new ArrayList<>();

            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                int[] colMap = new int[17];

                if (headerRow != null) {
                    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                        Cell cell = headerRow.getCell(c);
                        excelHeaders.add(cell != null ? cell.toString().trim() : "Col_" + c);
                    }
                    for (int i = 0; i < 17; i++) {
                        colMap[i] = excelHeaders.indexOf(EXCEL_HEADERS[i]);
                    }
                }

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    List<String> values = new ArrayList<>();
                    boolean hasData = false;
                    for (int i = 0; i < 17; i++) {
                        String val = "";
                        if (colMap[i] >= 0) {
                            Cell cell = row.getCell(colMap[i]);
                            if (cell != null) {
                                if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                                    val = cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                } else {
                                    val = cell.toString().trim();
                                }
                            }
                        }
                        values.add(val);
                        if (!val.isEmpty() && !"-".equals(val) && !"None".equals(val)) hasData = true;
                    }
                    if (hasData) allRows.add(values);
                }
            }

            int duplicados = 0;
            if (!allRows.isEmpty()) {
                List<String> payoutIds = allRows.stream().map(r -> r.get(1)).filter(id -> !id.isEmpty()).toList();
                if (!payoutIds.isEmpty()) {
                    String placeholders = String.join(",", payoutIds.stream().map(id -> "?").toArray(String[]::new));
                    Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM module_ct_excel_payouts WHERE payout_id IN (" + placeholders + ")",
                        Integer.class, payoutIds.toArray());
                    duplicados = count != null ? count : 0;
                }
            }

            String fechaMin = null, fechaMax = null;
            boolean solapamiento = false;
            boolean fechasDuplicadas = false;
            if (!allRows.isEmpty()) {
                fechaMin = allRows.get(0).get(0);
                fechaMax = allRows.get(allRows.size() - 1).get(0);
                if (fechaMin != null && fechaMin.contains("T")) {
                    LocalDateTime min = parseDateTime(fechaMin);
                    LocalDateTime max = fechaMax != null ? parseDateTime(fechaMax) : null;
                    if (min != null && max != null) {
                        fechaMin = min.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                        fechaMax = max.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                        Integer overlap = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM module_ct_excel_payouts WHERE upload_date BETWEEN ? AND ?",
                            Integer.class, Timestamp.valueOf(min), Timestamp.valueOf(max));
                        solapamiento = overlap != null && overlap > 0;
                    }
                }
            }

            // Check exact date duplicates (same upload_date day as existing data)
            if (!allRows.isEmpty()) {
                List<java.sql.Date> distinctDates = allRows.stream()
                    .map(r -> r.get(0))
                    .filter(d -> d != null && !d.isEmpty() && !"-".equals(d) && !"None".equals(d))
                    .map(this::parseDateTime)
                    .filter(d -> d != null)
                    .map(d -> java.sql.Date.valueOf(d.toLocalDate()))
                    .distinct()
                    .toList();
                if (!distinctDates.isEmpty()) {
                    String placeholders = String.join(",", distinctDates.stream().map(d -> "?").toArray(String[]::new));
                    Integer exact = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM module_ct_excel_payouts WHERE upload_date::date IN (" + placeholders + ")",
                        Integer.class, distinctDates.toArray());
                    fechasDuplicadas = exact != null && exact > 0;
                }
            }

            cache.put(cargaId, allRows);
            if (fechasDuplicadas) {
                bloqueados.put(cargaId, true);
            }

            jdbcTemplate.update(
                "INSERT INTO carga_masiva_historial (carga_id, file_name, total_filas, fecha_min, fecha_max, estado) VALUES (?,?,?,?::timestamp,?::timestamp,'preview')",
                cargaId, file.getOriginalFilename(), allRows.size(),
                fechaMin != null ? fechaMin : null,
                fechaMax != null ? fechaMax : null);

            List<List<String>> preview = allRows.subList(0, Math.min(20, allRows.size()));

            return CargaMasivaPreviewResponse.builder()
                    .cargaId(cargaId)
                    .fileName(file.getOriginalFilename())
                    .totalFilas(allRows.size())
                    .headers(List.of(EXCEL_HEADERS))
                    .preview(preview)
                    .fechaMin(fechaMin)
                    .fechaMax(fechaMax)
                    .duplicados(duplicados)
                    .solapamiento(solapamiento)
                    .fechasDuplicadas(fechasDuplicadas)
                    .mensaje(allRows.size() + " filas · " + duplicados + " duplicados"
                        + (fechasDuplicadas ? " · ⛔ Fechas ya existen en BD" : "")
                        + (!fechasDuplicadas && solapamiento ? " · ⚠ Fechas solapadas" : ""))
                    .build();

        } catch (Exception e) {
            log.error("[CargaMasiva] Error: {}", e.getMessage(), e);
            return CargaMasivaPreviewResponse.builder().mensaje("Error: " + e.getMessage()).build();
        }
    }

    @Override
    @Transactional
    public CargaMasivaImportResponse importExcel(String cargaId) {
        if (Boolean.TRUE.equals(bloqueados.get(cargaId))) {
            return CargaMasivaImportResponse.builder()
                    .importado(false)
                    .mensaje("⛔ No se puede importar: las fechas del archivo ya existen en la base de datos.")
                    .build();
        }

        List<List<String>> rows = cache.get(cargaId);
        if (rows == null || rows.isEmpty()) {
            return CargaMasivaImportResponse.builder()
                    .importado(false).mensaje("No hay datos. Sube el archivo primero.").build();
        }

        int count = 0;
        int skip = 0;
        int batchSize = 500;

        try {
            List<Object[]> batch = new ArrayList<>();
            for (List<String> row : rows) {
                Object[] params = new Object[17];
                for (int i = 0; i < 17 && i < row.size(); i++) {
                    String val = row.get(i);
                    if (val == null || val.isEmpty() || "-".equals(val) || "None".equals(val)) {
                        params[i] = null;
                    } else if (i == 0) {
                        params[i] = parseDateTime(val);
                    } else if (i == 8 || i == 9) {
                        try { params[i] = new BigDecimal(val); } catch (Exception e) { params[i] = null; }
                    } else {
                        params[i] = val;
                    }
                }
                for (int i = row.size(); i < 17; i++) params[i] = null;
                batch.add(params);

                if (batch.size() >= batchSize) {
                    int[] result = jdbcTemplate.batchUpdate(INSERT_SQL, batch);
                    for (int r : result) { if (r > 0) count++; else skip++; }
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                int[] result = jdbcTemplate.batchUpdate(INSERT_SQL, batch);
                for (int r : result) { if (r > 0) count++; else skip++; }
            }

            cache.remove(cargaId);

            jdbcTemplate.update(
                "UPDATE carga_masiva_historial SET filas_insertadas=?, duplicados_omitidos=?, estado='completado' WHERE carga_id=?",
                count, skip, cargaId);

            log.info("[CargaMasiva] {} insertados, {} omitidos, cargaId={}", count, skip, cargaId);

            return CargaMasivaImportResponse.builder()
                    .importado(true).filasImportadas(count).cargaId(cargaId)
                    .mensaje(count + " insertados" + (skip > 0 ? ", " + skip + " duplicados omitidos" : ""))
                    .build();

        } catch (Exception e) {
            log.error("[CargaMasiva] Error importando: {}", e.getMessage(), e);
            return CargaMasivaImportResponse.builder()
                    .importado(false).filasImportadas(count)
                    .mensaje("Error: " + e.getMessage()).build();
        }
    }

    @Override
    public List<CargaHistorialResponse> getHistorial() {
        return jdbcTemplate.query(
            "SELECT id, carga_id, file_name, total_filas, filas_insertadas, duplicados_omitidos, " +
            "COALESCE(to_char(fecha_min, 'YYYY-MM-DD HH24:MI'), '') as fecha_min, " +
            "COALESCE(to_char(fecha_max, 'YYYY-MM-DD HH24:MI'), '') as fecha_max, " +
            "estado, COALESCE(to_char(created_at, 'YYYY-MM-DD HH24:MI:SS'), '') as created_at " +
            "FROM carga_masiva_historial ORDER BY created_at DESC LIMIT 50",
            (rs, rowNum) -> CargaHistorialResponse.builder()
                .id(rs.getLong("id"))
                .cargaId(rs.getString("carga_id"))
                .fileName(rs.getString("file_name"))
                .totalFilas(rs.getInt("total_filas"))
                .filasInsertadas(rs.getInt("filas_insertadas"))
                .duplicadosOmitidos(rs.getInt("duplicados_omitidos"))
                .fechaMin(rs.getString("fecha_min"))
                .fechaMax(rs.getString("fecha_max"))
                .estado(rs.getString("estado"))
                .createdAt(rs.getString("created_at"))
                .build());
    }

    private LocalDateTime parseDateTime(String val) {
        for (DateTimeFormatter fmt : FORMATTERS) {
            try { return LocalDateTime.parse(val, fmt); } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
