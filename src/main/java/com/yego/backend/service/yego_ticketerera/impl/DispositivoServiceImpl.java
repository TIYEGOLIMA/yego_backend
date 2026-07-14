package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.config.JwtTokenProvider;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearDispositivoRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.DispositivoResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo;
import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo.TipoDispositivo;
import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import com.yego.backend.entity.yego_ticketerera.entities.Sede;
import com.yego.backend.repository.yego_ticketerera.DispositivoRepository;
import com.yego.backend.repository.yego_ticketerera.ModuloAtencionRepository;
import com.yego.backend.repository.yego_ticketerera.SedeRepository;
import com.yego.backend.service.yego_ticketerera.DispositivoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispositivoServiceImpl implements DispositivoService {

    private final DispositivoRepository dispositivoRepository;
    private final SedeRepository sedeRepository;
    private final ModuloAtencionRepository moduloAtencionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.device-expiration:2592000}")
    private long deviceTokenExpirationSeconds;

    @Override
    @Transactional(readOnly = true)
    public List<DispositivoResponse> listarDispositivos() {
        List<Dispositivo> dispositivos = dispositivoRepository.findByActiveTrueOrderByNameAsc();
        if (dispositivos.isEmpty()) return List.of();

        Map<Long, String> sedesMap = cargarSedesPorIds(
                dispositivos.stream().map(Dispositivo::getSedeId).collect(Collectors.toSet()));
        Map<Long, String> modulosMap = cargarModulosPorIds(
                dispositivos.stream()
                        .map(Dispositivo::getModuleId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet()));

        return dispositivos.stream()
                .map(d -> DispositivoResponse.from(
                        d,
                        sedesMap.get(d.getSedeId()),
                        d.getModuleId() != null ? modulosMap.get(d.getModuleId()) : null))
                .toList();
    }

    @Override
    @Transactional
    public DispositivoResponse crearDispositivo(CrearDispositivoRequest request) {
        Sede sede = obtenerSedeOFalla(request.getSedeId());
        ModuloAtencion modulo = validarModuloParaDispositivo(request.getType(), request.getModuleId(), sede.getId());

        String rawToken = generarTokenAcceso();
        Dispositivo saved = dispositivoRepository.save(Dispositivo.builder()
                .name(request.getName())
                .type(request.getType())
                .sedeId(sede.getId())
                .moduleId(modulo != null ? modulo.getId() : null)
                .description(request.getDescription())
                .accessToken(passwordEncoder.encode(rawToken))
                .active(true)
                .build());

        log.info("[Dispositivo] Creado: {} tipo={} sede={} modulo={}",
                saved.getName(), saved.getType(), saved.getSedeId(), saved.getModuleId());

        DispositivoResponse response = DispositivoResponse.from(
                saved, sede.getName(), modulo != null ? modulo.getName() : null);
        response.setAccessTokenPlain(rawToken);
        return response;
    }

    @Override
    @Transactional
    public DispositivoResponse actualizarDispositivo(Long id, CrearDispositivoRequest request) {
        Dispositivo dispositivo = obtenerDispositivoOFalla(id);
        Sede sede = obtenerSedeOFalla(request.getSedeId());
        ModuloAtencion modulo = validarModuloParaDispositivo(request.getType(), request.getModuleId(), sede.getId());

        dispositivo.setName(request.getName());
        dispositivo.setType(request.getType());
        dispositivo.setSedeId(sede.getId());
        dispositivo.setModuleId(modulo != null ? modulo.getId() : null);
        dispositivo.setDescription(request.getDescription());

        Dispositivo saved = dispositivoRepository.save(dispositivo);
        return DispositivoResponse.from(saved, sede.getName(), modulo != null ? modulo.getName() : null);
    }

    @Override
    @Transactional
    public DispositivoResponse regenerarTokenAcceso(Long id) {
        Dispositivo dispositivo = obtenerDispositivoOFalla(id);

        String rawToken = generarTokenAcceso();
        dispositivo.setAccessToken(passwordEncoder.encode(rawToken));
        int versionAnterior = dispositivo.getTokenVersion() != null ? dispositivo.getTokenVersion() : 0;
        dispositivo.setTokenVersion(versionAnterior + 1);
        Dispositivo saved = dispositivoRepository.save(dispositivo);
        log.info("[Dispositivo] Token regenerado id={} (tokenVersion={})", id, saved.getTokenVersion());

        String sedeNombre = sedeRepository.findById(saved.getSedeId()).map(Sede::getName).orElse(null);
        String moduleNombre = saved.getModuleId() != null
                ? moduloAtencionRepository.findById(saved.getModuleId()).map(ModuloAtencion::getName).orElse(null)
                : null;

        DispositivoResponse response = DispositivoResponse.from(saved, sedeNombre, moduleNombre);
        response.setAccessTokenPlain(rawToken);
        return response;
    }

    @Override
    @Transactional
    public DispositivoResponse asignarModulo(Long dispositivoId, Long moduleId) {
        Dispositivo dispositivo = obtenerDispositivoOFalla(dispositivoId);
        ModuloAtencion modulo = validarModuloParaDispositivo(dispositivo.getType(), moduleId, dispositivo.getSedeId());

        dispositivo.setModuleId(modulo != null ? modulo.getId() : null);
        Dispositivo saved = dispositivoRepository.save(dispositivo);
        log.info("[Dispositivo] Módulo {} -> dispositivo {}", saved.getModuleId(), saved.getId());

        String sedeNombre = sedeRepository.findById(saved.getSedeId()).map(Sede::getName).orElse(null);
        return DispositivoResponse.from(saved, sedeNombre, modulo != null ? modulo.getName() : null);
    }

    @Override
    @Transactional
    public void desactivarDispositivo(Long id) {
        Dispositivo dispositivo = obtenerDispositivoOFalla(id);
        dispositivo.setActive(false);
        dispositivoRepository.save(dispositivo);
        log.info("[Dispositivo] Desactivado id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> autenticarDispositivo(String rawAccessToken) {
        Dispositivo dispositivo = dispositivoRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .filter(d -> passwordEncoder.matches(rawAccessToken, d.getAccessToken()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de acceso inválido"));

        String sedeNombre = sedeRepository.findById(dispositivo.getSedeId()).map(Sede::getName).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", generarJwtDispositivo(dispositivo));
        result.put("dispositivoId", dispositivo.getId());
        result.put("nombre", dispositivo.getName());
        result.put("tipo", dispositivo.getType());
        result.put("sedeId", dispositivo.getSedeId());
        result.put("sedeNombre", sedeNombre);
        result.put("moduleId", dispositivo.getModuleId());
        log.info("[Dispositivo] Auth exitosa: {} (id={})", dispositivo.getName(), dispositivo.getId());
        return result;
    }

    private static final char[] TOKEN_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 5;
    private static final int MAX_TOKEN_INTENTOS = 20;

    @Override
    public String generarTokenAcceso() {
        List<Dispositivo> activos = dispositivoRepository.findByActiveTrueOrderByNameAsc();
        for (int intento = 0; intento < MAX_TOKEN_INTENTOS; intento++) {
            String candidato = construirTokenAleatorio();
            boolean colisiona = activos.stream()
                    .map(Dispositivo::getAccessToken)
                    .filter(hash -> hash != null && !hash.isBlank())
                    .anyMatch(hash -> passwordEncoder.matches(candidato, hash));
            if (!colisiona) {
                return candidato;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo generar un token único, intente nuevamente");
    }

    private String construirTokenAleatorio() {
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH + 5);
        sb.append("DISP-");
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(TOKEN_ALPHABET[TOKEN_RANDOM.nextInt(TOKEN_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private Dispositivo obtenerDispositivoOFalla(Long id) {
        return dispositivoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo no encontrado con ID: " + id));
    }

    private Sede obtenerSedeOFalla(Long sedeId) {
        if (sedeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sede es obligatoria");
        }
        return sedeRepository.findById(sedeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada con ID: " + sedeId));
    }

    /**
     * Solo las {@code TABLET} de calificación pueden tener un módulo asignado, y este debe pertenecer
     * a la misma sede del dispositivo. Devuelve el módulo cargado o {@code null} si no aplica.
     */
    private ModuloAtencion validarModuloParaDispositivo(TipoDispositivo tipo, Long moduleId, Long sedeId) {
        if (moduleId == null) return null;

        if (tipo != TipoDispositivo.TABLET) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo las Tablet de Calificación pueden tener un módulo asignado");
        }

        ModuloAtencion modulo = moduloAtencionRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Módulo no encontrado con ID: " + moduleId));

        if (!sedeId.equals(modulo.getSedeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El módulo no pertenece a la sede seleccionada");
        }
        return modulo;
    }

    private Map<Long, String> cargarSedesPorIds(java.util.Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return sedeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Sede::getId, Sede::getName));
    }

    private Map<Long, String> cargarModulosPorIds(java.util.Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return moduloAtencionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ModuloAtencion::getId, ModuloAtencion::getName));
    }

    private String generarJwtDispositivo(Dispositivo dispositivo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("dispositivoId", dispositivo.getId());
        claims.put("sedeId", dispositivo.getSedeId());
        claims.put("tipo", dispositivo.getType().name());
        claims.put("moduleId", dispositivo.getModuleId());
        claims.put("tokenVersion", dispositivo.getTokenVersion() != null ? dispositivo.getTokenVersion() : 0);
        return jwtTokenProvider.generate(
                "device:" + dispositivo.getId(), claims, deviceTokenExpirationSeconds);
    }
}
