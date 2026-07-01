package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_pro_ops.api.response.FleetVehicleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.VehicleTraceEvent;
import com.yego.backend.entity.yego_pro_ops.api.response.VehicleResponse;
import com.yego.backend.entity.yego_pro_ops.entities.*;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_pro_ops.*;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.MinIOService;
import com.yego.backend.service.yego_garantizado.FlotaService;
import com.yego.backend.service.yego_pro_ops.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private static final String YANGO_VEHICLES_URL = "https://fleet.yango.com/api/fleet/vehicles-manager/v1/vehicles/list";
    private static final String YANGO_DETAILS_URL = "https://fleet.yango.com/api/api/v1/cards/car/details";
    private static final String YANGO_QC_HISTORY_URL = "https://fleet.yango.com/api/fleet/fleet-quality-control/v2/rqc/history";

    private static final String YANGO_COOKIE_TEMPLATE = "i=LTfkD3HfqRCxqohgZeQ9z+FvXH7O5pWNsr8FmcsxS4KKmU5iKT51F+CoSQTst9xR7wVucbl5sJXpT0hebl/bCi+uGh4=; yandexuid=8271762941782249884; yashr=7968351331782249884; lang=es-419; yuidss=8271762941782249884; ymex=2097609886.yrts.1782249886; gdpr=0; _ym_uid=178224988651060427; _ym_d=1782249887; _ym_isad=2; Session_id=3:1782250583.5.0.1782250583600:WbD9Jg:dc7a.1.2:1|2223153146.0.2.0:3.3:1782250583|60:11994002.97621.FaHKxYH5WeraxZ6s71vAYcqDLtI; sessar=1.1908701.CiCgDFkhEsxSAGLwP8SPMEZyPVQbEX-x4JEQ7tsZcHngFg.LlYYj4MWDSkWSuh4Q-XXYja70AMqv8R5CL2OvkuSGi8; sessionid2=3:1782250583.5.0.1782250583600:WbD9Jg:dc7a.1.2:1|2223153146.0.2.0:3.3:1782250583|60:11994002.97621.fakesign0000000000000000000; yp=2097610583.udn.cDpnaW9tYXJvcnRlZ2E%3D; ys=udn.cDpnaW9tYXJvcnRlZ2E%3D; L=XQVWX3Z3c3hSXVpCRmx3cm1Qf1RXTXdSPh9aAAcTHgMyPAIt.1782250583.1984663.382117.d587626a363cd04b15a46df63ba6b5a2; yandex_login=giomarortega; _yasc=GdjDMpYx1FqzF4nDG4+00/3B57OdsK96k5suLEJ9EFBt76kpdjdus1d0VIgpvnrc5ZjuGlS8qA==; park_id=64085dd85e124e2c808806f70d527ea8";

    private final VehicleDocumentRepository documentRepository;
    private final VehicleMaintenanceRepository maintenanceRepository;
    private final VehicleMileageRepository mileageRepository;
    private final VehicleIncidentRepository incidentRepository;
    private final FleetSegmentRepository fleetSegmentRepository;
    private final FleetVehicleRepository fleetVehicleRepository;
    private final FleetVehicleHistoryRepository fleetVehicleHistoryRepository;
    private final FlotaService flotaService;
    private final MinIOService minIOService;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String BUCKET_DOCUMENTACION = "documentacion-flota";
    private static final String EVT_DOC_CARGADO = "DOC_CARGADO";
    private static final String EVT_DOC_ELIMINADO = "DOC_ELIMINADO";

    private HttpHeaders crearHeaders(String parkId) {
        String cookie = YANGO_COOKIE_TEMPLATE.replaceFirst("park_id=[a-f0-9]+", "park_id=" + parkId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", cookie);
        headers.set("x-park-id", parkId);
        return headers;
    }

    @Override
    public Map<String, Object> listarVehiculosYango(String parkId, String cursor) {
        try {
            HttpHeaders headers = crearHeaders(parkId);
            Map<String, Object> body = new HashMap<>();
            body.put("limit", 30);
            body.put("query", Map.of("car", Map.of(
                    "status", List.of(),
                    "categories", List.of(),
                    "owner", "park"
            )));
            if (cursor != null && !cursor.isBlank()) {
                body.put("cursor", cursor);
            }
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(YANGO_VEHICLES_URL, HttpMethod.POST, request, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("total", 0, "cars", List.of());
        } catch (Exception e) {
            log.error("Error listando vehículos Yango: {}", e.getMessage());
            return Map.of("total", 0, "cars", List.of());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public VehicleResponse obtenerDetalleVehiculo(String carId, String parkId) {
        try {
            HttpHeaders headers = crearHeaders(parkId);
            Map<String, Object> body = Map.of("car_id", carId);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(YANGO_DETAILS_URL, HttpMethod.POST, request, Map.class);

            Map<String, Object> respBody = response.getBody();
            if (respBody == null) return null;

            Map<String, Object> car = (Map<String, Object>) respBody.get("car");
            if (car == null) return null;

            VehicleResponse.YangoStatus yangoStatus = null;
            Object statusObj = car.get("status");
            if (statusObj instanceof Map) {
                Map<String, Object> statusMap = (Map<String, Object>) statusObj;
                yangoStatus = VehicleResponse.YangoStatus.builder()
                        .id((String) statusMap.get("id"))
                        .name((String) statusMap.get("name"))
                        .build();
            } else if (statusObj instanceof String) {
                yangoStatus = VehicleResponse.YangoStatus.builder()
                        .id((String) statusObj)
                        .name((String) statusObj)
                        .build();
            }

            VehicleResponse resp = VehicleResponse.builder()
                    .id(carId)
                    .parkId(parkId)
                    .brand((String) car.get("brand"))
                    .model((String) car.get("model"))
                    .year(car.get("year") != null ? ((Number) car.get("year")).intValue() : null)
                    .color((String) car.get("color"))
                    .colorName((String) car.get("color_name"))
                    .number((String) car.get("number"))
                    .callsign((String) car.get("callsign"))
                    .vin((String) car.get("vin"))
                    .status(yangoStatus)
                    .categories((List<String>) car.get("categories"))
                    .amenities((List<String>) car.get("amenities"))
                    .mileage(car.get("mileage") != null ? ((Number) car.get("mileage")).intValue() : 0)
                    .rental((Boolean) car.get("rental"))
                    .createdDate((String) car.get("created_date"))
                    .modifiedDate((String) car.get("modified_date"))
                    .build();

            resp.setDocuments(obtenerDocumentos(carId));
            resp.setMaintenance(obtenerMantenimientos(carId));
            resp.setMileageHistory(obtenerKilometraje(carId));
            resp.setIncidents(obtenerSiniestros(carId));

            // Foto frontal desde el QC history más reciente
            resp.setFotoUrl(obtenerFotoFrontal(carId, parkId));

            return resp;
        } catch (Exception e) {
            log.error("Error obteniendo detalle vehículo {}: {}", carId, e.getMessage());
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerHistorialQc(String carId, String parkId) {
        try {
            HttpHeaders headers = crearHeaders(parkId);
            Map<String, Object> body = new HashMap<>();
            body.put("car_id", carId);
            body.put("limit", 10);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(YANGO_QC_HISTORY_URL, HttpMethod.POST, request, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("items", List.of());
        } catch (Exception e) {
            log.error("Error obteniendo QC history {}: {}", carId, e.getMessage());
            return Map.of("items", List.of());
        }
    }

    // ── Flota cacheada (segmentación) ──

    @Override
    public List<FleetVehicleResponse> listarVehiculosGuardados(java.util.UUID segmentId) {
        List<FleetVehicle> vehiculos = (segmentId != null)
                ? fleetVehicleRepository.findBySegment_IdAndActivoTrue(segmentId)
                : fleetVehicleRepository.findByActivoTrue();

        Map<String, String> nombresPorPark = resolverNombresPark();

        return vehiculos.stream()
                .map(v -> toFleetVehicleResponse(v, nombresPorPark))
                .collect(Collectors.toList());
    }

    @Override
    public int sincronizarTodas() {
        int total = 0;
        for (FleetSegment segment : fleetSegmentRepository.findByActivoTrue()) {
            total += sincronizarSegmento(segment);
        }
        return total;
    }

    @Override
    public int sincronizarFlota(java.util.UUID segmentId) {
        FleetSegment segment = fleetSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("Flota no encontrada: " + segmentId));
        return sincronizarSegmento(segment);
    }

    private int sincronizarSegmento(FleetSegment segment) {
        int procesados = 0;
        String cursor = null;
        try {
            do {
                Map<String, Object> page = listarVehiculosYango(segment.getParkId(), cursor);
                Object carsObj = page.get("cars");
                if (carsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cars = (List<Map<String, Object>>) carsObj;
                    for (Map<String, Object> car : cars) {
                        upsertVehicle(segment, car);
                        procesados++;
                    }
                }
                cursor = (String) page.get("cursor");
            } while (cursor != null && !cursor.isBlank());
            log.info("✅ Flota {} sincronizada: {} vehículos", segment.getParkId(), procesados);
        } catch (Exception e) {
            log.error("❌ Error sincronizando flota {}: {}", segment.getParkId(), e.getMessage());
        }
        return procesados;
    }

    @SuppressWarnings("unchecked")
    private void upsertVehicle(FleetSegment segment, Map<String, Object> car) {
        String carId = str(car.get("id"));
        if (carId == null) return;

        Optional<FleetVehicle> existente = fleetVehicleRepository.findById(carId);
        FleetVehicle vehicle = existente.orElseGet(() -> FleetVehicle.builder().yangoCarId(carId).build());

        // Detectar cambio de flota ANTES de sobrescribir el segmento.
        boolean esNuevo = existente.isEmpty();
        UUID segmentAnterior = (vehicle.getSegment() != null) ? vehicle.getSegment().getId() : null;
        boolean cambioFlota = !esNuevo && segmentAnterior != null && !segmentAnterior.equals(segment.getId());

        // Decidir si se refresca la foto: solo si falta o el vehículo cambió en Yango.
        String modifiedNueva = str(car.get("modified_date"));
        boolean fotoFaltante = vehicle.getFotoUrl() == null || vehicle.getFotoUrl().isBlank();
        boolean cambioEnYango = modifiedNueva != null && !modifiedNueva.equals(vehicle.getModifiedDate());

        vehicle.setSegment(segment);
        vehicle.setNumber(str(car.get("number")));
        vehicle.setBrand(str(car.get("brand")));
        vehicle.setModel(str(car.get("model")));
        vehicle.setYear(car.get("year") != null ? ((Number) car.get("year")).intValue() : null);
        vehicle.setColor(str(car.get("color")));
        vehicle.setColorName(str(car.get("color_name")));
        vehicle.setVin(str(car.get("vin")));
        vehicle.setCallsign(str(car.get("callsign")));

        Object statusObj = car.get("status");
        if (statusObj instanceof Map) {
            Map<String, Object> statusMap = (Map<String, Object>) statusObj;
            vehicle.setStatusId(str(statusMap.get("id")));
            vehicle.setStatusName(str(statusMap.get("name")));
        } else if (statusObj instanceof String) {
            vehicle.setStatusId((String) statusObj);
            vehicle.setStatusName((String) statusObj);
        }

        vehicle.setCategories(toJson(car.get("categories")));
        vehicle.setAmenities(toJson(car.get("amenities")));
        vehicle.setMileage(car.get("mileage") != null ? ((Number) car.get("mileage")).intValue() : null);
        vehicle.setRental((Boolean) car.get("rental"));
        vehicle.setActivo(true);
        vehicle.setModifiedDate(modifiedNueva);
        vehicle.setSyncedAt(java.time.LocalDateTime.now());

        // Foto desde Yango (QC) solo si falta o el auto cambió; no sobrescribe una válida con null.
        if (fotoFaltante || cambioEnYango) {
            String foto = obtenerFotoFrontal(carId, segment.getParkId());
            if (foto != null) {
                vehicle.setFotoUrl(foto);
            }
        }

        fleetVehicleRepository.save(vehicle);

        if (esNuevo) {
            registrarHistorial(vehicle, null, segment, FleetVehicleHistory.TIPO_INGRESO);
        } else if (cambioFlota) {
            FleetSegment flotaAnterior = fleetSegmentRepository.findById(segmentAnterior).orElse(null);
            registrarHistorial(vehicle, flotaAnterior, segment, FleetVehicleHistory.TIPO_CAMBIO_FLOTA);
        }
    }

    private void registrarHistorial(FleetVehicle vehicle, FleetSegment anterior, FleetSegment nuevo, String tipo) {
        fleetVehicleHistoryRepository.save(FleetVehicleHistory.builder()
                .yangoCarId(vehicle.getYangoCarId())
                .number(vehicle.getNumber())
                .segmentIdAnterior(anterior != null ? anterior.getId() : null)
                .segmentIdNuevo(nuevo.getId())
                .parkIdAnterior(anterior != null ? anterior.getParkId() : null)
                .parkIdNuevo(nuevo.getParkId())
                .tipo(tipo)
                .build());
    }

    @Override
    public List<VehicleTraceEvent> obtenerTrazabilidad(String yangoCarId) {
        Map<String, String> nombresPorPark = resolverNombresPark();
        List<VehicleTraceEvent> eventos = new ArrayList<>();

        // 1) Eventos de flota (INGRESO / CAMBIO_FLOTA)
        for (FleetVehicleHistory h : fleetVehicleHistoryRepository.findByYangoCarIdOrderByCreatedAtDesc(yangoCarId)) {
            String flotaAnt = h.getParkIdAnterior() != null ? nombresPorPark.getOrDefault(h.getParkIdAnterior(), h.getParkIdAnterior()) : null;
            String flotaNue = nombresPorPark.getOrDefault(h.getParkIdNuevo(), h.getParkIdNuevo());
            String desc = FleetVehicleHistory.TIPO_INGRESO.equals(h.getTipo())
                    ? "Ingresó a la flota " + flotaNue
                    : "Cambió de flota: " + flotaAnt + " → " + flotaNue;
            eventos.add(VehicleTraceEvent.builder()
                    .tipo(h.getTipo()).descripcion(desc)
                    .flotaAnterior(flotaAnt).flotaNuevo(flotaNue)
                    .fecha(h.getCreatedAt()).build());
        }

        // 2) Eventos de documentos (cargado / eliminado), resolviendo nombres en batch
        List<VehicleDocument> docs = documentRepository.findByYangoCarIdOrderByCreatedAtAsc(yangoCarId);
        Set<Long> userIds = new HashSet<>();
        docs.forEach(d -> { if (d.getCreatedById() != null) userIds.add(d.getCreatedById()); if (d.getDeletedById() != null) userIds.add(d.getDeletedById()); });
        Map<Long, String> nombresUsuarios = resolverNombresUsuarios(userIds);

        for (VehicleDocument d : docs) {
            eventos.add(VehicleTraceEvent.builder()
                    .tipo(EVT_DOC_CARGADO)
                    .descripcion("Documento " + d.getTipo() + " cargado")
                    .usuario(d.getCreatedById() != null ? nombresUsuarios.get(d.getCreatedById()) : null)
                    .fecha(d.getCreatedAt())
                    .build());
            if (Boolean.TRUE.equals(d.getEliminado())) {
                eventos.add(VehicleTraceEvent.builder()
                        .tipo(EVT_DOC_ELIMINADO)
                        .descripcion("Documento " + d.getTipo() + " eliminado")
                        .usuario(d.getDeletedById() != null ? nombresUsuarios.get(d.getDeletedById()) : null)
                        .fecha(d.getDeletedAt())
                        .build());
            }
        }

        // Orden por fecha desc (nulls al final)
        eventos.sort(Comparator.comparing(VehicleTraceEvent::getFecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return eventos;
    }

    private Map<Long, String> resolverNombresUsuarios(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        Map<Long, String> map = new HashMap<>();
        for (User u : userRepository.findAllById(ids)) {
            String nombre = ((u.getName() != null ? u.getName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "")).trim();
            map.put(u.getId(), nombre.isBlank() ? u.getUsername() : nombre);
        }
        return map;
    }

    @Override
    public VehicleResponse obtenerDetallePorPlaca(String placa) {
        FleetVehicle vehicle = fleetVehicleRepository.findByNumberIgnoreCaseAndActivoTrue(placa)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado con placa: " + placa));
        String parkId = vehicle.getSegment().getParkId();
        VehicleResponse resp = obtenerDetalleVehiculo(vehicle.getYangoCarId(), parkId);
        if (resp != null) {
            resp.setSegmentId(vehicle.getSegment().getId());
        }
        return resp;
    }

    private FleetVehicleResponse toFleetVehicleResponse(FleetVehicle v, Map<String, String> nombresPorPark) {
        VehicleResponse.YangoStatus status = null;
        if (v.getStatusId() != null || v.getStatusName() != null) {
            status = VehicleResponse.YangoStatus.builder()
                    .id(v.getStatusId()).name(v.getStatusName()).build();
        }
        String parkId = v.getSegment() != null ? v.getSegment().getParkId() : null;
        return FleetVehicleResponse.builder()
                .id(v.getYangoCarId())
                .segmentId(v.getSegment() != null ? v.getSegment().getId() : null)
                .parkId(parkId)
                .parkNombre(parkId != null ? nombresPorPark.getOrDefault(parkId, parkId) : null)
                .number(v.getNumber())
                .brand(v.getBrand())
                .model(v.getModel())
                .year(v.getYear())
                .color(v.getColor())
                .colorName(v.getColorName())
                .vin(v.getVin())
                .callsign(v.getCallsign())
                .status(status)
                .categories(fromJson(v.getCategories()))
                .amenities(fromJson(v.getAmenities()))
                .mileage(v.getMileage())
                .rental(v.getRental())
                .fotoUrl(v.getFotoUrl())
                .build();
    }

    private Map<String, String> resolverNombresPark() {
        try {
            return flotaService.obtenerTodosLosPartners().stream()
                    .filter(p -> p.getId() != null && p.getName() != null)
                    .collect(Collectors.toMap(p -> p.getId(), p -> p.getName(), (a, b) -> a));
        } catch (Exception e) {
            log.warn("No se pudieron resolver nombres de park: {}", e.getMessage());
            return Map.of();
        }
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }

    /** Extrae la URL de la foto frontal del QC history más reciente del vehículo. */
    @SuppressWarnings("unchecked")
    private String obtenerFotoFrontal(String carId, String parkId) {
        try {
            Map<String, Object> qcResponse = obtenerHistorialQc(carId, parkId);
            List<Map<String, Object>> items = (List<Map<String, Object>>) qcResponse.get("items");
            if (items != null && !items.isEmpty()) {
                List<Map<String, Object>> media = (List<Map<String, Object>>) items.get(0).get("media");
                if (media != null) {
                    return media.stream()
                            .filter(m -> "front".equals(m.get("code")))
                            .findFirst()
                            .map(m -> "https://fleet.yango.com" + m.get("url"))
                            .orElse(null);
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener foto del vehículo {}: {}", carId, e.getMessage());
        }
        return null;
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<VehicleResponse.DocumentInfo> obtenerDocumentos(String yangoCarId) {
        return documentRepository.findByYangoCarIdAndEliminadoFalseOrderByFechaVigenteAsc(yangoCarId).stream()
                .map(this::toDocInfo).toList();
    }

    @Override
    @Transactional
    public VehicleResponse.DocumentInfo agregarDocumento(String yangoCarId, VehicleDocument doc) {
        doc.setYangoCarId(yangoCarId);
        if (doc.getEstado() == null) doc.setEstado("vigente");
        return toDocInfo(documentRepository.save(doc));
    }

    @Override
    @Transactional
    public VehicleResponse.DocumentInfo agregarDocumentoConArchivo(String yangoCarId, String tipo, String nombre,
                                                                   LocalDate fechaVigente, MultipartFile file, Long createdById) {
        long correlativo = documentRepository.countByYangoCarIdAndTipo(yangoCarId, tipo) + 1;
        String archivoUrl = subirADocumentacionFlota(yangoCarId, tipo, correlativo, file);

        VehicleDocument doc = VehicleDocument.builder()
                .yangoCarId(yangoCarId)
                .tipo(tipo)
                .nombre(nombre)
                .fechaVigente(fechaVigente)
                .archivoUrl(archivoUrl)
                .estado("vigente")
                .createdById(createdById)
                .build();
        return toDocInfo(documentRepository.save(doc));
    }

    @Override
    public String subirArchivoMantenimiento(String yangoCarId, MultipartFile file) {
        long correlativo = maintenanceRepository.countByYangoCarId(yangoCarId) + 1;
        return subirADocumentacionFlota(yangoCarId, "MANT", correlativo, file);
    }

    /** Sube el archivo al bucket documentacion-flota con nombre {placa}/{TIPO}-{correlativo}.{ext}. */
    private String subirADocumentacionFlota(String yangoCarId, String tipo, long correlativo, MultipartFile file) {
        String placa = fleetVehicleRepository.findById(yangoCarId)
                .map(FleetVehicle::getNumber)
                .filter(n -> n != null && !n.isBlank())
                .orElse(yangoCarId);
        String placaSlug = placa.replaceAll("[^A-Za-z0-9_-]", "");
        String tipoSlug = tipo == null ? "DOC" : tipo.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        String ext = extraerExtension(file.getOriginalFilename());
        // Nombre plano {placa}-{TIPO}-{correlativo}: el gateway antepone un UUID y conserva el basename.
        String objectName = placaSlug + "-" + tipoSlug + "-" + correlativo + (ext.isBlank() ? "" : "." + ext);

        String url = minIOService.subirArchivo(file, BUCKET_DOCUMENTACION, objectName);
        if (url == null) {
            throw new IllegalStateException("No se pudo subir el archivo a MinIO");
        }
        return url;
    }

    private String extraerExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1).toLowerCase() : "";
    }

    @Override
    @Transactional
    public void eliminarDocumento(Long docId, Long deletedById) {
        documentRepository.findById(docId).ifPresent(doc -> {
            doc.setEliminado(true);
            doc.setDeletedById(deletedById);
            doc.setDeletedAt(java.time.LocalDateTime.now());
            documentRepository.save(doc);
        });
    }

    @Override
    public List<VehicleResponse.MaintenanceInfo> obtenerMantenimientos(String yangoCarId) {
        return maintenanceRepository.findByYangoCarIdOrderByFechaDesc(yangoCarId).stream()
                .map(this::toMaintInfo).toList();
    }

    @Override
    @Transactional
    public VehicleResponse.MaintenanceInfo agregarMantenimiento(String yangoCarId, VehicleMaintenance mant) {
        mant.setYangoCarId(yangoCarId);
        if (mant.getEstado() == null) mant.setEstado("completado");
        return toMaintInfo(maintenanceRepository.save(mant));
    }

    @Override
    @Transactional
    public void eliminarMantenimiento(Long mantId) {
        maintenanceRepository.deleteById(mantId);
    }

    @Override
    public List<VehicleResponse.MileageInfo> obtenerKilometraje(String yangoCarId) {
        return mileageRepository.findByYangoCarIdOrderByFechaAsc(yangoCarId).stream()
                .map(this::toMileageInfo).toList();
    }

    @Override
    @Transactional
    public VehicleResponse.MileageInfo agregarKilometraje(String yangoCarId, VehicleMileage km) {
        km.setYangoCarId(yangoCarId);
        return toMileageInfo(mileageRepository.save(km));
    }

    @Override
    public List<VehicleResponse.IncidentInfo> obtenerSiniestros(String yangoCarId) {
        return incidentRepository.findByYangoCarIdOrderByFechaDesc(yangoCarId).stream()
                .map(this::toIncidentInfo).toList();
    }

    @Override
    @Transactional
    public VehicleResponse.IncidentInfo agregarSiniestro(String yangoCarId, VehicleIncident inc) {
        inc.setYangoCarId(yangoCarId);
        if (inc.getEstado() == null) inc.setEstado("reportado");
        return toIncidentInfo(incidentRepository.save(inc));
    }

    @Override
    @Transactional
    public void eliminarSiniestro(Long incId) {
        incidentRepository.deleteById(incId);
    }

    private VehicleResponse.DocumentInfo toDocInfo(VehicleDocument d) {
        return VehicleResponse.DocumentInfo.builder()
                .id(d.getId()).tipo(d.getTipo()).nombre(d.getNombre())
                .fechaVigente(d.getFechaVigente())
                .archivoUrl(d.getArchivoUrl()).estado(d.getEstado())
                .build();
    }

    private VehicleResponse.MaintenanceInfo toMaintInfo(VehicleMaintenance m) {
        return VehicleResponse.MaintenanceInfo.builder()
                .id(m.getId()).tipo(m.getTipo()).categoria(m.getCategoria())
                .fecha(m.getFecha()).kilometraje(m.getKilometraje())
                .descripcion(m.getDescripcion()).problema(m.getProblema())
                .diagnostico(m.getDiagnostico()).solucion(m.getSolucion())
                .taller(m.getTaller()).responsable(m.getResponsable())
                .costo(m.getCosto()).archivoUrl(m.getArchivoUrl())
                .estado(m.getEstado()).proximaFecha(m.getProximaFecha())
                .proximoKm(m.getProximoKm())
                .build();
    }

    private VehicleResponse.MileageInfo toMileageInfo(VehicleMileage m) {
        return VehicleResponse.MileageInfo.builder()
                .id(m.getId()).fecha(m.getFecha()).kilometraje(m.getKilometraje())
                .build();
    }

    private VehicleResponse.IncidentInfo toIncidentInfo(VehicleIncident i) {
        return VehicleResponse.IncidentInfo.builder()
                .id(i.getId()).fecha(i.getFecha()).tipo(i.getTipo())
                .descripcion(i.getDescripcion()).conductor(i.getConductor())
                .montoDano(i.getMontoDano()).estado(i.getEstado())
                .evidencias(i.getEvidencias())
                .build();
    }
}
