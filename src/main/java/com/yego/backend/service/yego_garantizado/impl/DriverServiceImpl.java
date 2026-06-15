package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.response.DriverInfo;
import com.yego.backend.entity.yego_garantizado.api.response.DriverValidationResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaDisponibleResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaInfo;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.entity.yego_api_externo.api.response.PPendientesResponse;
import com.yego.backend.entity.yego_garantizado.entities.Driver;
import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
import com.yego.backend.entity.yego_api_externo.entities.FleetCache;
import com.yego.backend.repository.yego_garantizado.DriverRepository;
import com.yego.backend.repository.yego_api_externo.FleetCacheRepository;
import com.yego.backend.service.yego_garantizado.DriverService;
import com.yego.backend.service.yego_garantizado.FlotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final FlotaService flotaService;
    private final FleetCacheRepository fleetCacheRepository;
    private final RestTemplate restTemplate;
    
    // Park IDs permitidos (filtro)
    private static final Set<String> PARK_IDS_PERMITIDOS = Set.of(
        "05b1c831e66f41a9a87f5f3fa0a186ae", // Yego Cali
        "08e20910d81d42658d4334d3f6d10ac0", // Yego Lima
        "56e4607dfc354e0a9cde4f0aa7973003", // Yego Arequipa
        "ef21f793358144f589aabcbeb8bd7d50", // Yego Barranquilla
        "c054c8b5dfe14e75b882943b2a252706", // Yego Black
        "c58110bc70244430a70a8126fc69f22c", // Yego Líderes
        "5921e55cc5d042d28747dd722608955a", // Yego Prime
        "ff424287c4bd4cbba6066962951a121f", // Yego Promi
        "851e30755bba4d298e2e837f571b4ab8", // Yego Trujillo
        "ae57aaedeacd41eb9fdbe1ff7a89a3f2", // Yego,
        "2e39f6699c854bc49cc75197431fe25c"  //Yego.
    );

    public DriverServiceImpl(DriverRepository driverRepository, FlotaService flotaService, FleetCacheRepository fleetCacheRepository, RestTemplate restTemplate) {
        this.driverRepository = driverRepository;
        this.flotaService = flotaService;
        this.fleetCacheRepository = fleetCacheRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public Optional<DriverInfo> validarYObtenerLicencia(String licenseNumber) {
        log.info("Validando y obteniendo datos de licencia: {}", licenseNumber);
        List<Driver> registros = driverRepository.findAllByLicenseNumber(licenseNumber);
        if (!registros.isEmpty()) {
            DriverInfo driverInfo = convertirADriverInfo(registros.get(0));
            log.info("Licencia {} encontrada - Conductor: {}", licenseNumber, driverInfo.getFullName());
            return Optional.of(driverInfo);
        }
        log.warn("Licencia {} no encontrada en la base de datos", licenseNumber);
        return Optional.empty();
    }

    @Override
    public DriverValidationResponse validarLicencia(String licenseNumber) {
        log.info("Validando licencia: {}", licenseNumber);
        List<Driver> registros = driverRepository.findAllByLicenseNumber(licenseNumber);
        List<DriverInfo> lista = registros.stream()
                .map(this::convertirADriverInfo)
                .collect(Collectors.toList());
        boolean existe = !lista.isEmpty();
        if (existe) {
            log.info("Licencia {} existe con {} coincidencias", licenseNumber, lista.size());
        } else {
            log.warn("Licencia {} no existe", licenseNumber);
        }
        return DriverValidationResponse.builder()
                .licenseNumber(licenseNumber)
                .existe(existe)
                .mensaje(existe ? "Coincidencias encontradas" : "Licencia no encontrada")
                .drivers(lista)
                .build();
    }
    
    @Override
    public FlotaDisponibleResponse obtenerConductorConFlotas(String licenseNumber) {
        log.info("Obteniendo conductor con flotas disponibles: {}", licenseNumber);
        
        // 1. Buscar TODOS los registros del conductor
        List<Driver> todosLosRegistros = driverRepository.findAllByLicenseNumber(licenseNumber);
        
        if (todosLosRegistros.isEmpty()) {
            log.warn("No se encontraron registros para la licencia: {}", licenseNumber);
            return FlotaDisponibleResponse.builder()
                    .licenseNumber(licenseNumber)
                    .conductor(null)
                    .flotasDisponibles(new ArrayList<>())
                    .mensaje("Licencia no encontrada")
                    .build();
        }
        
        // 2 Obtener info del conductor (del primer registro)
        Driver primerRegistro = todosLosRegistros.get(0);
        DriverInfo conductorInfo = convertirADriverInfo(primerRegistro);
        
        // 3. Filtrar solo los park_id permitidos
        Set<String> parkIdsDelConductor = todosLosRegistros.stream()
                .map(Driver::getParkId)
                .filter(PARK_IDS_PERMITIDOS::contains)
                .collect(Collectors.toSet());
        
        log.info("Conductor tiene {} flotas, {} son permitidas", 
                todosLosRegistros.size(), parkIdsDelConductor.size());
        
        // 4. Obtener información de flotas desde la API
        List<FlotaResponse> todasLasFlotas = flotaService.obtenerFlotas();
        
        // 5. Filtrar solo las flotas del conductor
        List<FlotaInfo> flotasDisponibles = todasLasFlotas.stream()
                .filter(flota -> parkIdsDelConductor.contains(flota.getId()))
                .map(flota -> FlotaInfo.builder()
                        .parkId(flota.getId())
                        .parkName(flota.getName())
                        .city(flota.getCity())
                        .activo(true)
                        .build())
                .collect(Collectors.toList());
        
        log.info("Flotas disponibles para {}: {}", licenseNumber, flotasDisponibles.size());
        
        return FlotaDisponibleResponse.builder()
                .licenseNumber(licenseNumber)
                .conductor(conductorInfo)
                .flotasDisponibles(flotasDisponibles)
                .mensaje("Conductor encontrado con " + flotasDisponibles.size() + " flotas disponibles")
                .build();
    }
    
    @Override
    public PPendientesResponse obtenerPendientes(String telefono) {
        log.info("📋 [DriverService] Obteniendo pagos pendientes para GoBot - Valor recibido: {}", telefono);
        
        // Validar que el parámetro esté presente
        if (telefono == null || telefono.trim().isEmpty()) {
            log.error("❌ [DriverService] Se requiere teléfono o licencia");
            return PPendientesResponse.builder()
                    .nombre("").flota("").monto(0.0).pagos(0).license("").surnames("")
                    .idcar("").placa("").iddriver("").telefonop("").idyego(null)
                    .idflota("").hireDate(null).estatus(400).message("Se requiere teléfono o licencia").msystem("").build();
        }
        
        String valorLimpio = telefono.trim();
        List<Object[]> resultados = new ArrayList<>();
        String telefonoParaBuscar = null;
        String licenciaParaBuscar = null;
        String driverIdParaBuscar = null;
        boolean esTelefono = false;
        boolean esLicencia = false;
        boolean esDriverId = false;
        
        // Detectar si es driver_id (longitud ~32 caracteres hexadecimales, puede ser un poco menor o mayor)
        // Ejemplo: "95bb09d0001a4f8fb3a7e3d4a0c7113b" (32 caracteres)
        if (valorLimpio.length() >= 28 && valorLimpio.length() <= 36 && valorLimpio.matches("^[0-9a-fA-F]+$")) {
            esDriverId = true;
            driverIdParaBuscar = valorLimpio;
            log.info("🆔 [DriverService] Detectado como driver_id: {}", driverIdParaBuscar);
            
            // Buscar por driver_id
            log.info("🆔 [DriverService] Buscando en BD por driver_id: {}", driverIdParaBuscar);
            resultados = driverRepository.findAllByDriverIdAsDriverApiNative(driverIdParaBuscar);
        }
        // Detectar si es teléfono o licencia
        // Un teléfono generalmente empieza con +, 51, 57, o es un número largo
        // Una licencia generalmente tiene letras o es más corta
        else if (valorLimpio.startsWith("+") || valorLimpio.startsWith("51") || valorLimpio.startsWith("57") || 
            (valorLimpio.length() >= 8 && valorLimpio.matches(".*\\d.*"))) {
            // Probablemente es un teléfono
            esTelefono = true;
            log.info("📱 [DriverService] Detectado como teléfono: {}", valorLimpio);
            
            // Normalizar teléfono
            if (valorLimpio.startsWith("+51") || valorLimpio.startsWith("+57")) {
                telefonoParaBuscar = valorLimpio;
                log.info("📱 [DriverService] Teléfono ya tiene prefijo internacional: {}", telefonoParaBuscar);
            } else if (valorLimpio.startsWith("51")) {
                telefonoParaBuscar = "+" + valorLimpio;
                log.info("📱 [DriverService] Teléfono empieza con 51, agregando '+': {}", telefonoParaBuscar);
            } else if (valorLimpio.startsWith("57")) {
                telefonoParaBuscar = "+" + valorLimpio;
                log.info("📱 [DriverService] Teléfono empieza con 57, agregando '+': {}", telefonoParaBuscar);
            } else if (valorLimpio.startsWith("+")) {
                String telefonoSinPlus = valorLimpio.substring(1);
                if (telefonoSinPlus.startsWith("9")) {
                    telefonoParaBuscar = "+51" + telefonoSinPlus;
                    log.info("📱 [DriverService] Teléfono con '+' empieza con 9, agregando prefijo +51 (Perú)");
                } else {
                    telefonoParaBuscar = "+57" + telefonoSinPlus;
                    log.info("📱 [DriverService] Teléfono con '+' no empieza con 9, agregando prefijo +57 (Colombia)");
                }
            } else {
                if (valorLimpio.startsWith("9")) {
                    telefonoParaBuscar = "+51" + valorLimpio;
                    log.info("📱 [DriverService] Teléfono empieza con 9, agregando prefijo +51 (Perú)");
                } else {
                    telefonoParaBuscar = "+57" + valorLimpio;
                    log.info("📱 [DriverService] Teléfono no empieza con 9, agregando prefijo +57 (Colombia)");
                }
            }
            
            // Buscar por teléfono primero
            log.info("📱 [DriverService] Buscando en BD por teléfono: {}", telefonoParaBuscar);
            resultados = driverRepository.findAllByPhoneAsDriverApiNative(telefonoParaBuscar);
        } else {
            // Probablemente es una licencia
            esLicencia = true;
            licenciaParaBuscar = valorLimpio;
            log.info("🪪 [DriverService] Detectado como licencia: {}", licenciaParaBuscar);
        }
        
        // Si no se encontró por teléfono, intentar como licencia
        if (resultados.isEmpty() && esTelefono) {
            log.info("🪪 [DriverService] No se encontró por teléfono, intentando como licencia: {}", valorLimpio);
            licenciaParaBuscar = valorLimpio;
            resultados = driverRepository.findAllByLicenseAsDriverApiNative(licenciaParaBuscar);
            esLicencia = true;
        }
        
        // Si se detectó como licencia desde el inicio, buscar por licencia
        if (resultados.isEmpty() && esLicencia && licenciaParaBuscar != null) {
            log.info("🪪 [DriverService] Buscando por licencia: {}", licenciaParaBuscar);
            resultados = driverRepository.findAllByLicenseAsDriverApiNative(licenciaParaBuscar);
        }
        
        if (resultados.isEmpty() && !esDriverId) {
            String mensajeError = esTelefono && esLicencia 
                ? "Conductor no encontrado por teléfono ni por licencia"
                : esTelefono 
                    ? "Conductor no encontrado por teléfono"
                    : "Conductor no encontrado por licencia";
            
            return PPendientesResponse.builder()
                    .nombre("").flota("").monto(0.0).pagos(0).license("").surnames("")
                    .idcar("").placa("").iddriver("").telefonop(telefonoParaBuscar != null ? telefonoParaBuscar : "").idyego(null)
                    .idflota("").hireDate(null).estatus(400).message(mensajeError).msystem("").build();
        }
        
        if (resultados.isEmpty() && esDriverId) {
            return PPendientesResponse.builder()
                    .nombre("").flota("").monto(0.0).pagos(0).license("").surnames("")
                    .idcar("").placa("").iddriver(driverIdParaBuscar).telefonop("").idyego(null)
                    .idflota("").hireDate(null).estatus(400).message("Conductor no encontrado por driver_id").msystem("").build();
        }
        
        // La query ya ordena primero los que tienen park_id no nulo
        // Solo necesitamos tomar el primero de la lista
        Object[] resultadoSeleccionado = resultados.get(0);
        
        // Verificar si tiene park_id para logging
        boolean tieneParkId = resultadoSeleccionado.length > 1 && 
                             resultadoSeleccionado[1] != null && 
                             !resultadoSeleccionado[1].toString().trim().isEmpty();
        
        if (tieneParkId) {
            log.info("✅ [DriverService] Se seleccionó conductor con park_id: {} y driver_id: {}", 
                    resultadoSeleccionado[1], resultadoSeleccionado[0]);
        } else {
            log.warn("⚠️ [DriverService] No se encontró conductor con park_id, usando el primero disponible con driver_id: {}", 
                    resultadoSeleccionado[0]);
        }
        
        DriverApi driver = mapearObjectArrayADriverApi(resultadoSeleccionado);
        
        // Obtener nombre de flota desde cache local (fallback a API externa si no disponible)
        String nombreFlota = getFleetName(driver.getParkId());
        
        // Nombre y apellidos
        String nombre = driver.getFirstName() != null ? driver.getFirstName() : 
                       (driver.getFullName() != null ? driver.getFullName().split(" ", 2)[0] : "");
        String surnames = driver.getFullName() != null ? driver.getFullName() : "";
        
        // Usar el teléfono del conductor encontrado si no se proporcionó uno, o el normalizado si se proporcionó
        // Si se buscó por driver_id, devolver el teléfono solo en ese caso
        String telefonoFinal;
        if (esDriverId) {
            // Si se buscó por driver_id, devolver el teléfono
            telefonoFinal = driver.getPhone() != null ? driver.getPhone() : "";
            log.info("📱 [DriverService] Teléfono encontrado por driver_id: {}", telefonoFinal);
        } else {
            // Para teléfono o licencia, usar el teléfono encontrado o el normalizado
            telefonoFinal = driver.getPhone() != null ? driver.getPhone() : 
                          (telefonoParaBuscar != null ? telefonoParaBuscar : "");
        }
        
        // Construir respuesta
        return PPendientesResponse.builder()
                .nombre(nombre)
                .flota(nombreFlota)
                .monto(0.0)
                .pagos(0)
                .license(driver.getLicenseNumber() != null ? driver.getLicenseNumber() : "")
                .surnames(surnames)
                .idcar(driver.getCarId() != null ? driver.getCarId() : "")
                .placa(driver.getCarNumber() != null ? driver.getCarNumber() : "")
                .iddriver(driver.getDriverId() != null ? driver.getDriverId() : "")
                .telefonop(telefonoFinal)
                .idyego(null)
                .idflota(driver.getParkId() != null ? driver.getParkId() : "")
                .hireDate(driver.getHireDate())
                .estatus(200)
                .message("Se verifica que no tiene cuenta bancaria registrada.\n\n*Estado:* No cuenta con pagos pendientes\n")
                .msystem("")
                .build();
    }
    
    /**
     * Mapea un array de Object[] a DriverApi
     * Solo mapea los campos necesarios: driver_id, park_id, first_name, full_name, phone, license_number, car_id, car_number, hire_date
     */
    private DriverApi mapearObjectArrayADriverApi(Object[] row) {
        DriverApi driver = new DriverApi();
        int index = 0;
        
        driver.setDriverId(row[index++] != null ? row[index-1].toString() : null);
        driver.setParkId(row[index++] != null ? row[index-1].toString() : null);
        driver.setFirstName(row[index++] != null ? row[index-1].toString() : null);
        driver.setFullName(row[index++] != null ? row[index-1].toString() : null);
        driver.setPhone(row[index++] != null ? row[index-1].toString() : null);
        driver.setLicenseNumber(row[index++] != null ? row[index-1].toString() : null);
        driver.setCarId(row[index++] != null ? row[index-1].toString() : null);
        driver.setCarNumber(row[index++] != null ? row[index-1].toString() : null);
        if (row.length > index && row[index] != null) {
            driver.setHireDate(java.time.LocalDate.parse(row[index].toString()));
        }
        index++;
        
        return driver;
    }

    private String getFleetName(String parkId) {
        try {
            return fleetCacheRepository.findById(parkId)
                    .map(fc -> {
                        log.info("✅ [DriverService] Fleet cache HIT para park_id '{}': {}", parkId, fc.getName());
                        return fc.getName();
                    })
                    .orElseGet(() -> refreshFleetCacheAndGetName(parkId));
        } catch (Exception e) {
            log.warn("⚠️ [DriverService] Fleet cache no disponible, usando API externa como fallback: {}", e.getMessage());
            return obtenerFlotasPendientes().stream()
                    .filter(f -> f.getId().equals(parkId))
                    .findFirst()
                    .map(FlotaResponse::getName)
                    .orElse("N/A");
        }
    }

    private String refreshFleetCacheAndGetName(String parkId) {
        log.info("🔄 [DriverService] park_id '{}' no encontrado en cache, poblando desde API externa", parkId);
        List<FlotaResponse> flotas = obtenerFlotasPendientes();
        for (FlotaResponse f : flotas) {
            FleetCache fc = FleetCache.builder()
                    .parkId(f.getId())
                    .name(f.getName())
                    .city(f.getCity())
                    .build();
            fleetCacheRepository.save(fc);
        }
        log.info("💾 [DriverService] Fleet cache actualizado con {} flotas desde API externa", flotas.size());
        return fleetCacheRepository.findById(parkId)
                .map(FleetCache::getName)
                .orElse("N/A");
    }

    @Override
    public List<FlotaResponse> obtenerFlotasPendientes() {
        try {
            log.info("🔍 Obteniendo todas las flotas desde API externa (sin filtrar)");
            String url = "http://162.55.214.109:6000/v2/partners";
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
            
            List<FlotaResponse> flotas = new ArrayList<>();
            
            if (response != null && response.containsKey("partners")) {
                List<Map<String, Object>> partners = (List<Map<String, Object>>) response.get("partners");
                
                // Convertir TODAS las flotas sin filtrar
                for (Map<String, Object> item : partners) {
                    FlotaResponse flota = new FlotaResponse();
                    flota.setId(item.get("id").toString());
                    flota.setName(item.get("name").toString());
                    flota.setCity(item.get("city") != null ? item.get("city").toString() : null);
                    flota.setSpecifications((List<String>) item.get("specifications"));
                    flotas.add(flota);
                    log.info("✅ Flota agregada: {} - {}", flota.getId(), flota.getName());
                }
            }
            
            log.info("✅ Total de flotas obtenidas: {} (TODAS sin filtrar)", flotas.size());
            return flotas;
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo flotas: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private DriverInfo convertirADriverInfo(Driver driver) {
        DriverInfo info = new DriverInfo();
        info.setDriverId(driver.getDriverId());
        info.setFullName(driver.getFullName());
        info.setPhone(driver.getPhone());
        info.setRating(driver.getRating());
        info.setLicenseNumber(driver.getLicenseNumber());
        return info;
    }
}

