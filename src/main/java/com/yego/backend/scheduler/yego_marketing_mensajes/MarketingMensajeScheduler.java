package com.yego.backend.scheduler.yego_marketing_mensajes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import com.yego.backend.repository.yego_marketing_mensajes.MarketingMensajeRepository;
import com.yego.backend.service.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MarketingMensajeScheduler {

    private static final String YANGO_API_URL = "https://fleet.yango.com/api/fleet/communications/v2/mailings";

    private static final String YANGO_COOKIE_TEMPLATE = "yandexuid=3749697061773160569; yashr=4439789851773160569; yuidss=3749697061773160569; receive-cookie-deprecation=1; _ym_uid=177316057294224056; _ym_d=1773160572; gdpr=0; Session_id=3:1774364989.5.0.1774364989907:WbD9Jg:2ba1.1.2:1|2221285626.0.2.0:3.3:1774364989|60:11796558.594160.n_Jmlkg1gEuCZt_1XvVWkN4dw8c; sessar=1.1719225.CiBVRVJOGbtkgeSto1PnCayxyvxUggJdZy4wgPsQQWcnsA.2wfis8ZmdUDtbnJ2950KmXXfVO-vBnzfZpSYDEBg9GI; sessionid2=3:1774364989.5.0.1774364989907:WbD9Jg:2ba1.1.2:1|2221285626.0.2.0:3.3:1774364989|60:11796558.594160.fakesign0000000000000000000; L=XDBnAWBleFpjWXRwR1dCdndTf2BGemB6JgIzVhRDLDAHKjk=.1774364989.1789215.35875.8178e2f5138ad768bc37ca50165d71c0; yandex_login=soporteyego; park_id=08e20910d81d42658d4334d3f6d10ac0; i=fIZ5U8gv+BnmXssVOW+Rlw1xyUJL/CWPG2TSElX4phVAKslCk8dz7O4prgqdu4OexuQa9s0TDXYfKRrSLV7aeDbPExg=; _ym_isad=1; yp=2089724989.udn.cDpzb3BvcnRleWVnbw%3D%3D#1776348466.yu.3749697061773160569; ymex=1778854066.oyu.3749697061773160569#2088520571.yrts.1773160571; _ym_visorc=b; _yasc=H84k3iGSVVnqKHr+W6o7G1DpIEo6Mk7LZszfQxKXf6X47rDMjr36qt54pkY4JfNpxcidNA==; bh=EkAiR29vZ2xlIENocm9tZSI7dj0iMTQ3IiwgIk5vdC5BL0JyYW5kIjt2PSI4IiwgIkNocm9taXVtIjt2PSIxNDciGgN4ODYiDTE0Ny4wLjc3MjcuNTUqAj8wOgciTGludXgiSgI2NFJYIkdvb2dsZSBDaHJvbWUiO3Y9IjE0Ny4wLjc3MjcuNTUiLCJOb3QuQS9CcmFuZCI7dj0iOC4wLjAuMCIsIkNocm9taXVtIjt2PSIxNDcuMC43NzI3LjU1ImDAqv/OBmoZ3MrpiA7yrLelC/v68OcN6//99g+bh8+HCA==";

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final int VENTANA_MINUTOS = 5;
    private static final long DELAY_ENTRE_FLOTAS_MS = 5_000L;

    private static final Map<DayOfWeek, String> DIAS_SEMANA = Map.of(
            DayOfWeek.MONDAY, "Lun",
            DayOfWeek.TUESDAY, "Mar",
            DayOfWeek.WEDNESDAY, "Mié",
            DayOfWeek.THURSDAY, "Jue",
            DayOfWeek.FRIDAY, "Vie",
            DayOfWeek.SATURDAY, "Sáb",
            DayOfWeek.SUNDAY, "Dom"
    );

    private static final Map<String, String> DIAS_ALTERNATIVOS = Map.of(
            "Mie", "Mié",
            "Sab", "Sáb"
    );

    private final MarketingMensajeRepository marketingMensajeRepository;
    private final WhatsAppService whatsAppService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Environment environment;

    /** Clave: mensajeId, Valor: última hora (HH:mm) en que se envió, para evitar duplicados dentro de la misma ventana. */
    private final Map<Long, String> ultimoEnvioPorMensaje = new HashMap<>();

    public MarketingMensajeScheduler(MarketingMensajeRepository marketingMensajeRepository,
                                     WhatsAppService whatsAppService,
                                     ObjectMapper objectMapper,
                                     RestTemplate restTemplate,
                                     Environment environment) {
        this.marketingMensajeRepository = marketingMensajeRepository;
        this.whatsAppService = whatsAppService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.environment = environment;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void verificarYEnviarMensajesProgramados() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("dev") || activeProfiles.contains("local")) {
            log.info("⏭️ [MarketingMensajeScheduler] Omitido en modo desarrollo (profiles: {})", activeProfiles);
            return;
        }
        log.info("⏰ [MarketingMensajeScheduler] Scheduler ejecutándose - Verificando mensajes programados");
        try {
            LocalTime horaActual = LocalTime.now(ZONA_LIMA);
            String diaActual = DIAS_SEMANA.get(LocalDate.now(ZONA_LIMA).getDayOfWeek());

            List<String> horasVentana = new ArrayList<>(VENTANA_MINUTOS);
            for (int i = 0; i < VENTANA_MINUTOS; i++) {
                horasVentana.add(horaActual.minusMinutes(i).format(HORA_FORMATTER));
            }

            List<MarketingMensaje> mensajes = marketingMensajeRepository
                    .findByActivoTrueAndHorasEspecificasIsNotNull()
                    .stream()
                    .filter(m -> tieneDestinatarios(m))
                    .collect(Collectors.toList());

            if (mensajes.isEmpty()) return;

            for (MarketingMensaje mensaje : mensajes) {
                procesarMensajeSiCorresponde(mensaje, horasVentana, diaActual);
            }

            evictarCacheSiNecesario();
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeScheduler] Error en verificación de mensajes programados: {}",
                    e.getMessage(), e);
        }
    }

    private boolean tieneDestinatarios(MarketingMensaje mensaje) {
        return esTextoNoVacio(mensaje.getGrupos()) || esTextoNoVacio(mensaje.getFlotas());
    }

    private void procesarMensajeSiCorresponde(MarketingMensaje mensaje, List<String> horasVentana, String diaActual) {
        try {
            Map<String, List<String>> horasPorDia = objectMapper.readValue(
                    mensaje.getHorasEspecificas(),
                    new TypeReference<Map<String, List<String>>>() {}
            );

            for (String hora : horasVentana) {
                List<String> diasProgramados = horasPorDia.get(hora);
                if (diasProgramados == null) continue;

                boolean diaCoincide = diasProgramados.stream()
                        .anyMatch(d -> normalizarDia(d).equals(normalizarDia(diaActual)));

                if (!diaCoincide) continue;
                if (hora.equals(ultimoEnvioPorMensaje.get(mensaje.getId()))) continue;

                enviarMensajeProgramado(mensaje);
                ultimoEnvioPorMensaje.put(mensaje.getId(), hora);
                break;
            }
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeScheduler] Error parseando horas del mensaje {}: {}",
                    mensaje.getId(), e.getMessage());
        }
    }

    private void enviarMensajeProgramado(MarketingMensaje mensaje) {
        log.info("📤 [MarketingMensajeScheduler] Enviando mensaje ID: {} - Título: {}", mensaje.getId(), mensaje.getTitulo());
        try {
            List<String> grupos = parseJsonList(mensaje.getGrupos());
            List<String> flotas = parseJsonList(mensaje.getFlotas());

            if (grupos.isEmpty() && flotas.isEmpty()) {
                log.warn("⚠️ [MarketingMensajeScheduler] Mensaje ID {} sin grupos ni flotas", mensaje.getId());
                return;
            }

            if (!grupos.isEmpty()) {
                String nombreArchivo = extraerNombreArchivo(mensaje.getArchivo());
                whatsAppService.enviarAMultiplesGrupos(
                        grupos,
                        mensaje.getMensaje(),
                        mensaje.getArchivo(),
                        nombreArchivo,
                        mensaje.getTipo()
                );
            }

            if (!flotas.isEmpty()) {
                enviarMensajeAYango(mensaje, flotas);
            }
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeScheduler] Error enviando mensaje ID {}: {}",
                    mensaje.getId(), e.getMessage(), e);
        }
    }

    private void enviarMensajeAYango(MarketingMensaje mensaje, List<String> flotas) {
        List<String> parkIds = extraerParkIdsUnicos(flotas);
        if (parkIds.isEmpty()) return;

        Map<String, Object> body = Map.of(
                "type", "pro",
                "title", mensaje.getTitulo(),
                "message", mensaje.getMensaje(),
                "recipients", Map.of("filters", Map.of())
        );

        int exitosos = 0, fallidos = 0, omitidos = 0;

        for (int i = 0; i < parkIds.size(); i++) {
            String parkId = parkIds.get(i);
            try {
                HttpHeaders headers = crearHeadersFleet(parkId);
                ResponseEntity<Void> response = restTemplate.exchange(
                        YANGO_API_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    exitosos++;
                    log.info("✅ [MarketingMensajeScheduler] Fleet OK - Park ID: {}", parkId);
                } else {
                    fallidos++;
                    log.error("❌ [MarketingMensajeScheduler] Fleet fallo - Park ID: {} | Status: {}",
                            parkId, response.getStatusCode().value());
                }
            } catch (HttpClientErrorException e) {
                int status = e.getStatusCode().value();
                String body2 = e.getResponseBodyAsString();
                if (status == 400 && body2 != null && body2.contains("limit_time")) {
                    omitidos++;
                    log.warn("⚠️ [MarketingMensajeScheduler] Fleet limit_time - Park ID: {}", parkId);
                } else if (status == 403) {
                    fallidos++;
                    log.warn("⚠️ [MarketingMensajeScheduler] Fleet 403 sin permiso - Park ID: {}", parkId);
                } else {
                    fallidos++;
                    log.error("❌ [MarketingMensajeScheduler] Fleet error - Park ID: {} | {} | {}",
                            parkId, status, body2);
                }
            } catch (HttpServerErrorException e) {
                fallidos++;
                log.error("❌ [MarketingMensajeScheduler] Fleet 5xx - Park ID: {} | {} | {}",
                        parkId, e.getStatusCode().value(), e.getResponseBodyAsString());
            } catch (Exception e) {
                fallidos++;
                log.error("❌ [MarketingMensajeScheduler] Fleet error general - Park ID: {} | {}",
                        parkId, e.getMessage(), e);
            }

            pausarEntreFlotas(i, parkIds.size());
        }

        log.info("📊 [MarketingMensajeScheduler] Resumen Fleet - Mensaje ID: {} | ✅ {} | ❌ {} | ⏳ {} | Total: {}",
                mensaje.getId(), exitosos, fallidos, omitidos, parkIds.size());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private HttpHeaders crearHeadersFleet(String parkId) {
        String cookie = YANGO_COOKIE_TEMPLATE.replaceFirst("park_id=[a-f0-9]+", "park_id=" + parkId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", cookie);
        headers.set("x-park-id", parkId);
        headers.set("X-Idempotency-Token", UUID.randomUUID().toString());
        return headers;
    }

    private List<String> extraerParkIdsUnicos(List<String> flotas) {
        return flotas.stream()
                .filter(Objects::nonNull)
                .flatMap(f -> Arrays.stream(f.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> parseJsonList(String json) {
        if (!esTextoNoVacio(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("⚠️ [MarketingMensajeScheduler] Error parseando JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String normalizarDia(String dia) {
        if (dia == null) return "";
        return DIAS_ALTERNATIVOS.getOrDefault(dia, dia);
    }

    private static boolean esTextoNoVacio(String s) {
        return s != null && !s.isBlank();
    }

    private String extraerNombreArchivo(String url) {
        if (url == null || url.isEmpty()) return null;
        int slash = url.lastIndexOf('/');
        if (slash < 0 || slash >= url.length() - 1) return null;
        String name = url.substring(slash + 1);
        int q = name.indexOf('?');
        return q > 0 ? name.substring(0, q) : name;
    }

    private void pausarEntreFlotas(int indiceActual, int total) {
        if (indiceActual >= total - 1) return;
        try {
            Thread.sleep(DELAY_ENTRE_FLOTAS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void evictarCacheSiNecesario() {
        if (ultimoEnvioPorMensaje.size() > 100) {
            ultimoEnvioPorMensaje.clear();
        }
    }
}
