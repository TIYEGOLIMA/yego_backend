package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_pro_ops.api.response.VehicleResponse;
import com.yego.backend.entity.yego_pro_ops.entities.*;
import com.yego.backend.repository.yego_pro_ops.*;
import com.yego.backend.service.yego_pro_ops.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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

            // Extraer foto frontal del QC history más reciente
            Map<String, Object> qcResponse = obtenerHistorialQc(carId, parkId);
            List<Map<String, Object>> items = (List<Map<String, Object>>) qcResponse.get("items");
            if (items != null && !items.isEmpty()) {
                List<Map<String, Object>> media = (List<Map<String, Object>>) items.get(0).get("media");
                if (media != null) {
                    media.stream()
                        .filter(m -> "front".equals(m.get("code")))
                        .findFirst()
                        .ifPresent(m -> resp.setFotoUrl("https://fleet.yango.com" + m.get("url")));
                }
            }

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

    @Override
    public List<VehicleResponse.DocumentInfo> obtenerDocumentos(String yangoCarId) {
        return documentRepository.findByYangoCarIdOrderByFechaFinAsc(yangoCarId).stream()
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
    public void eliminarDocumento(Long docId) {
        documentRepository.deleteById(docId);
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
                .fechaInicio(d.getFechaInicio()).fechaFin(d.getFechaFin())
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
