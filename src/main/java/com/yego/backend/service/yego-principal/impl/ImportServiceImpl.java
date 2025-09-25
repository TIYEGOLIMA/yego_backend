package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.entity.yego_principal.entities.Import;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.ImportRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_principal.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de importaciones del sistema YEGO Principal
 * Equivalente a ImportsService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {
    
    private final ImportRepository importRepository;
    private final UserRepository userRepository;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    
    @Override
    @Transactional
    public ImportUploadResponseDto uploadFile(MultipartFile file, UploadImportDto uploadImportDto, Long userId) {
        // Validar tipo de importación
        if (!Arrays.asList("users", "roles", "permissions").contains(uploadImportDto.getType())) {
            throw new IllegalArgumentException("Tipo de importación no válido: " + uploadImportDto.getType());
        }
        
        // Crear registro de importación
        Import importRecord = Import.builder()
                .userId(userId)
                .filename(uploadImportDto.getFilename())
                .type(Import.ImportType.valueOf(uploadImportDto.getType().toUpperCase()))
                .status(Import.ImportStatus.PENDING)
                .build();
        
        Import savedImport = importRepository.save(importRecord);
        
        log.info("✅ Importación YEGO Principal creada: {}", savedImport.getFilename());
        
        // Procesar archivo CSV para obtener vista previa
        try {
            CsvProcessResult result = processCsvFile(file, uploadImportDto.getType());
            
            // Actualizar importación con vista previa
            savedImport.setPreview(result.getPreview());
            // Convertir Map a JSON string
            savedImport.setErrors(result.getErrors() != null ? 
                result.getErrors().toString() : null);
            savedImport.setTotalRows(result.getTotalRows());
            
            importRepository.save(savedImport);
            
            return ImportUploadResponseDto.builder()
                    .message("Archivo subido y procesado exitosamente")
                    .importId(savedImport.getId())
                    .preview(result.getPreview())
                    .errors(result.getErrors() != null ? 
                        result.getErrors().toString() : null)
                    .totalRows(result.getTotalRows())
                    .build();
                    
        } catch (IOException e) {
            log.error("Error procesando archivo CSV YEGO Principal: {}", e.getMessage());
            throw new RuntimeException("Error procesando archivo CSV", e);
        }
    }
    
    @Override
    public List<ImportResponseDto> findAll(Long userId, String startDate, String endDate) {
        List<Import> imports;
        
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        
        // Parsear fechas si se proporcionan
        if (startDate != null && !startDate.trim().isEmpty()) {
            startDateTime = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            endDateTime = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59);
        }
        
        // Aplicar filtros
        if (userId != null && startDateTime != null && endDateTime != null) {
            imports = importRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDateTime, endDateTime);
        } else if (userId != null && startDateTime != null) {
            imports = importRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, startDateTime);
        } else if (userId != null && endDateTime != null) {
            imports = importRepository.findByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(userId, endDateTime);
        } else if (userId != null) {
            imports = importRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else if (startDateTime != null && endDateTime != null) {
            imports = importRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDateTime, endDateTime);
        } else if (startDateTime != null) {
            imports = importRepository.findByCreatedAtAfterOrderByCreatedAtDesc(startDateTime);
        } else if (endDateTime != null) {
            imports = importRepository.findByCreatedAtBeforeOrderByCreatedAtDesc(endDateTime);
        } else {
            imports = importRepository.findAllByOrderByCreatedAtDesc();
        }
        
        return imports.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public ImportResponseDto findOne(Long id) {
        Import importRecord = importRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Importación con ID " + id + " no encontrada"));
        
        return mapToResponseDto(importRecord);
    }
    
    @Override
    public ImportPreviewDto getPreview(Long id) {
        Import importRecord = importRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Importación con ID " + id + " no encontrada"));
        
        if (importRecord.getPreview() == null) {
            throw new IllegalStateException("No hay vista previa disponible para esta importación");
        }
        
        return ImportPreviewDto.builder()
                .id(importRecord.getId())
                .filename(importRecord.getFilename())
                .type(importRecord.getType().name().toLowerCase())
                .totalRows(importRecord.getTotalRows())
                .preview(importRecord.getPreview())
                .errors(importRecord.getErrors())
                .build();
    }
    
    @Override
    @Transactional
    public ImportResponseDto processImport(Long id, ProcessImportDto processImportDto) {
        Import importRecord = importRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Importación con ID " + id + " no encontrada"));
        
        if (importRecord.getStatus() != Import.ImportStatus.PENDING) {
            throw new IllegalStateException("La importación ya fue procesada");
        }
        
        importRecord.setStatus(Import.ImportStatus.PROCESSING);
        importRepository.save(importRecord);
        
        try {
            // Simular procesamiento (en una implementación real se procesarían los datos)
            simulateProcessing(importRecord, processImportDto);
            
            importRecord.setStatus(Import.ImportStatus.COMPLETED);
            importRecord.setProcessedRows(importRecord.getTotalRows());
            
            int errorCount = importRecord.getErrors() != null ? 
                (importRecord.getErrors().isEmpty() ? 0 : 1) : 0;
            importRecord.setSuccessRows(importRecord.getTotalRows() - errorCount);
            importRecord.setErrorRows(errorCount);
            
            Import savedImport = importRepository.save(importRecord);
            
            log.info("✅ Importación YEGO Principal procesada: {}", savedImport.getFilename());
            
            return mapToResponseDto(savedImport);
            
        } catch (Exception e) {
            importRecord.setStatus(Import.ImportStatus.FAILED);
            importRepository.save(importRecord);
            
            log.error("❌ Error procesando importación YEGO Principal: {}", e.getMessage());
            throw new RuntimeException("Error procesando importación", e);
        }
    }
    
    @Override
    @Transactional
    public void remove(Long id) {
        Import importRecord = importRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Importación con ID " + id + " no encontrada"));
        
        importRepository.delete(importRecord);
        
        log.info("✅ Importación YEGO Principal eliminada: {}", importRecord.getFilename());
    }
    
    private CsvProcessResult processCsvFile(MultipartFile file, String type) throws IOException {
        List<Object> preview = new ArrayList<>();
        Map<String, Object> errors = new HashMap<>();
        int totalRows = 0;
        
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            for (CSVRecord record : csvParser) {
                totalRows++;
                
                try {
                    ValidationResult validationResult = validateRow(record, type, totalRows);
                    
                    if (validationResult.isValid()) {
                        if (preview.size() < 10) { // Solo las primeras 10 filas para preview
                            Map<String, Object> rowData = new HashMap<>();
                            rowData.put("id", totalRows);
                            rowData.putAll(validationResult.getData());
                            preview.add(rowData);
                        }
                    } else {
                        errors.put(String.valueOf(totalRows), validationResult.getErrors());
                    }
                    
                } catch (Exception e) {
                    errors.put(String.valueOf(totalRows), Arrays.asList("Error de validación: " + e.getMessage()));
                }
            }
        }
        
        return CsvProcessResult.builder()
                .preview(preview)
                .errors(errors)
                .totalRows(totalRows)
                .build();
    }
    
    private ValidationResult validateRow(CSVRecord record, String type, int rowNumber) {
        switch (type.toLowerCase()) {
            case "users":
                return validateUserRow(record, rowNumber);
            case "roles":
                return validateRoleRow(record, rowNumber);
            case "permissions":
                return validatePermissionRow(record, rowNumber);
            default:
                return ValidationResult.invalid(Arrays.asList("Tipo de importación no válido"));
        }
    }
    
    private ValidationResult validateUserRow(CSVRecord record, int rowNumber) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        
        // Validar campos requeridos
        String firstName = record.get("first_name");
        if (firstName == null || firstName.trim().isEmpty()) {
            errors.add("first_name es requerido");
        } else {
            data.put("firstName", firstName.trim());
        }
        
        String lastName = record.get("last_name");
        if (lastName == null || lastName.trim().isEmpty()) {
            errors.add("last_name es requerido");
        } else {
            data.put("lastName", lastName.trim());
        }
        
        String email = record.get("email");
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("email es requerido y debe ser válido");
        } else {
            data.put("email", email.trim().toLowerCase());
        }
        
        String nationalId = record.get("national_id");
        if (nationalId == null || nationalId.trim().isEmpty()) {
            errors.add("national_id es requerido");
        } else {
            data.put("dni", nationalId.trim());
        }
        
        String role = record.get("role");
        if (role == null || role.trim().isEmpty()) {
            errors.add("role es requerido");
        } else {
            data.put("role", role.trim());
        }
        
        return errors.isEmpty() ? ValidationResult.valid(data) : ValidationResult.invalid(errors);
    }
    
    private ValidationResult validateRoleRow(CSVRecord record, int rowNumber) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        
        String name = record.get("name");
        if (name == null || name.trim().isEmpty()) {
            errors.add("name es requerido");
        } else {
            data.put("name", name.trim());
        }
        
        String description = record.get("description");
        if (description == null || description.trim().isEmpty()) {
            errors.add("description es requerido");
        } else {
            data.put("description", description.trim());
        }
        
        return errors.isEmpty() ? ValidationResult.valid(data) : ValidationResult.invalid(errors);
    }
    
    private ValidationResult validatePermissionRow(CSVRecord record, int rowNumber) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        
        String name = record.get("name");
        if (name == null || name.trim().isEmpty()) {
            errors.add("name es requerido");
        } else {
            data.put("name", name.trim());
        }
        
        String module = record.get("module");
        if (module == null || module.trim().isEmpty()) {
            errors.add("module es requerido");
        } else {
            data.put("module", module.trim());
        }
        
        String action = record.get("action");
        if (action == null || action.trim().isEmpty()) {
            errors.add("action es requerido");
        } else {
            data.put("action", action.trim());
        }
        
        String description = record.get("description");
        if (description != null && !description.trim().isEmpty()) {
            data.put("description", description.trim());
        }
        
        return errors.isEmpty() ? ValidationResult.valid(data) : ValidationResult.invalid(errors);
    }
    
    private void simulateProcessing(Import importRecord, ProcessImportDto processImportDto) {
        // Simular procesamiento
        try {
            Thread.sleep(2000); // Simular tiempo de procesamiento
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // En una implementación real, aquí se procesarían los datos:
        // - Crear usuarios, roles, permisos según el tipo
        // - Validar duplicados
        // - Manejar errores específicos
    }
    
    private ImportResponseDto mapToResponseDto(Import importRecord) {
        ImportResponseDto.ImportUserDto userDto = null;
        
        if (importRecord.getUser() != null) {
            User user = importRecord.getUser();
            userDto = ImportResponseDto.ImportUserDto.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();
        }
        
        return ImportResponseDto.builder()
                .id(importRecord.getId())
                .userId(importRecord.getUserId())
                .filename(importRecord.getFilename())
                .status(importRecord.getStatus().name().toLowerCase())
                .totalRows(importRecord.getTotalRows())
                .processedRows(importRecord.getProcessedRows())
                .successRows(importRecord.getSuccessRows())
                .errorRows(importRecord.getErrorRows())
                .errors(importRecord.getErrors())
                .preview(importRecord.getPreview())
                .type(importRecord.getType().name().toLowerCase())
                .createdAt(importRecord.getCreatedAt())
                .updatedAt(importRecord.getUpdatedAt())
                .user(userDto)
                .build();
    }
    
    // Clases auxiliares
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class CsvProcessResult {
        private List<Object> preview;
        private Map<String, Object> errors;
        private Integer totalRows;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ValidationResult {
        private boolean valid;
        private Map<String, Object> data;
        private List<String> errors;
        
        public static ValidationResult valid(Map<String, Object> data) {
            return ValidationResult.builder()
                    .valid(true)
                    .data(data)
                    .build();
        }
        
        public static ValidationResult invalid(List<String> errors) {
            return ValidationResult.builder()
                    .valid(false)
                    .errors(errors)
                    .build();
        }
    }
}
