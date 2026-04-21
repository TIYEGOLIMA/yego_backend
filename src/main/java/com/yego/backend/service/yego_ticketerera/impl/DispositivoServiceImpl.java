package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearDispositivoRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.DispositivoResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo;
import com.yego.backend.entity.yego_ticketerera.entities.Sede;
import com.yego.backend.repository.yego_ticketerera.DispositivoRepository;
import com.yego.backend.repository.yego_ticketerera.SedeRepository;
import com.yego.backend.service.yego_ticketerera.DispositivoService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispositivoServiceImpl implements DispositivoService {

    private final DispositivoRepository dispositivoRepository;
    private final SedeRepository sedeRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:28800}")
    private Long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispositivoResponse> listarDispositivos() {
        return dispositivoRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(d -> {
                    String sedeNombre = sedeRepository.findById(d.getSedeId())
                            .map(Sede::getName).orElse(null);
                    return DispositivoResponse.from(d, sedeNombre);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispositivoResponse> listarDispositivosPorSede(Long sedeId) {
        String sedeNombre = sedeRepository.findById(sedeId).map(Sede::getName).orElse(null);
        return dispositivoRepository.findBySedeIdAndActiveTrueOrderByNameAsc(sedeId)
                .stream()
                .map(d -> DispositivoResponse.from(d, sedeNombre))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DispositivoResponse obtenerDispositivo(Long id) {
        Dispositivo d = dispositivoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado con ID: " + id));
        String sedeNombre = sedeRepository.findById(d.getSedeId()).map(Sede::getName).orElse(null);
        return DispositivoResponse.from(d, sedeNombre);
    }

    @Override
    @Transactional
    public DispositivoResponse crearDispositivo(CrearDispositivoRequest request) {
        sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + request.getSedeId()));

        String rawToken = generarTokenAcceso();
        String hashedToken = passwordEncoder.encode(rawToken);

        Dispositivo dispositivo = Dispositivo.builder()
                .name(request.getName())
                .type(request.getType())
                .sedeId(request.getSedeId())
                .moduleId(request.getModuleId())
                .description(request.getDescription())
                .accessToken(hashedToken)
                .active(true)
                .build();

        Dispositivo saved = dispositivoRepository.save(dispositivo);
        log.info("[Dispositivo] Creado: {} tipo={} sede={}", saved.getName(), saved.getType(), saved.getSedeId());

        String sedeNombre = sedeRepository.findById(saved.getSedeId()).map(Sede::getName).orElse(null);
        DispositivoResponse response = DispositivoResponse.from(saved, sedeNombre);
        // Retornar el token en plaintext solo en la creación
        response.setDescription(response.getDescription() != null
                ? response.getDescription() + " | TOKEN: " + rawToken
                : "TOKEN: " + rawToken);
        return response;
    }

    @Override
    @Transactional
    public DispositivoResponse actualizarDispositivo(Long id, CrearDispositivoRequest request) {
        Dispositivo dispositivo = dispositivoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado con ID: " + id));

        sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + request.getSedeId()));

        dispositivo.setName(request.getName());
        dispositivo.setType(request.getType());
        dispositivo.setSedeId(request.getSedeId());
        dispositivo.setModuleId(request.getModuleId());
        dispositivo.setDescription(request.getDescription());

        Dispositivo saved = dispositivoRepository.save(dispositivo);
        String sedeNombre = sedeRepository.findById(saved.getSedeId()).map(Sede::getName).orElse(null);
        return DispositivoResponse.from(saved, sedeNombre);
    }

    @Override
    @Transactional
    public void desactivarDispositivo(Long id) {
        Dispositivo dispositivo = dispositivoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado con ID: " + id));
        dispositivo.setActive(false);
        dispositivoRepository.save(dispositivo);
        log.info("[Dispositivo] Desactivado id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> autenticarDispositivo(String rawAccessToken) {
        // Buscar todos los dispositivos activos y comparar con BCrypt
        Dispositivo dispositivo = dispositivoRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .filter(d -> passwordEncoder.matches(rawAccessToken, d.getAccessToken()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Token de acceso inválido"));

        String sedeNombre = sedeRepository.findById(dispositivo.getSedeId())
                .map(Sede::getName).orElse(null);

        String jwt = generarJwtDispositivo(dispositivo);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", jwt);
        result.put("dispositivoId", dispositivo.getId());
        result.put("nombre", dispositivo.getName());
        result.put("tipo", dispositivo.getType());
        result.put("sedeId", dispositivo.getSedeId());
        result.put("sedeNombre", sedeNombre);
        result.put("moduleId", dispositivo.getModuleId());
        log.info("[Dispositivo] Auth exitosa: {} (id={})", dispositivo.getName(), dispositivo.getId());
        return result;
    }

    @Override
    public String generarTokenAcceso() {
        return "DISP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String generarJwtDispositivo(Dispositivo dispositivo) {
        long expirationMs = jwtExpiration * 1000L;
        return Jwts.builder()
                .claim("dispositivoId", dispositivo.getId())
                .claim("sedeId", dispositivo.getSedeId())
                .claim("tipo", dispositivo.getType().name())
                .claim("moduleId", dispositivo.getModuleId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }
}
