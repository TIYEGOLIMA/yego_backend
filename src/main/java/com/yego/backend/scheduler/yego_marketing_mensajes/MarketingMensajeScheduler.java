package com.yego.backend.scheduler.yego_marketing_mensajes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import com.yego.backend.repository.yego_marketing_mensajes.MarketingMensajeRepository;
import com.yego.backend.service.yego_marketing_mensajes.sender.MarketingDeliveryResult;
import com.yego.backend.service.yego_marketing_mensajes.sender.MarketingFleetSender;
import com.yego.backend.service.yego_marketing_mensajes.sender.MarketingWhatsAppSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MarketingMensajeScheduler {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final int VENTANA_MINUTOS = 5;

    private static final Map<DayOfWeek, String> DIAS_SEMANA = Map.of(
            DayOfWeek.MONDAY, "Lun",
            DayOfWeek.TUESDAY, "Mar",
            DayOfWeek.WEDNESDAY, "Mié",
            DayOfWeek.THURSDAY, "Jue",
            DayOfWeek.FRIDAY, "Vie",
            DayOfWeek.SATURDAY, "Sáb",
            DayOfWeek.SUNDAY, "Dom");

    private static final Map<String, String> DIAS_ALTERNATIVOS = Map.of(
            "Mie", "Mié",
            "Sab", "Sáb");

    private final MarketingMensajeRepository marketingMensajeRepository;
    private final MarketingWhatsAppSender whatsAppSender;
    private final MarketingFleetSender fleetSender;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public MarketingMensajeScheduler(
            MarketingMensajeRepository marketingMensajeRepository,
            MarketingWhatsAppSender whatsAppSender,
            MarketingFleetSender fleetSender,
            ObjectMapper objectMapper,
            Environment environment) {
        this.marketingMensajeRepository = marketingMensajeRepository;
        this.whatsAppSender = whatsAppSender;
        this.fleetSender = fleetSender;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Scheduled(
            fixedDelayString = "${marketing.scheduler.fixed-delay-ms:300000}",
            initialDelayString = "${marketing.scheduler.initial-delay-ms:60000}")
    public void verificarYEnviarMensajesProgramados() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("dev") || activeProfiles.contains("local")) {
            log.debug("[MarketingScheduler] Omitido en desarrollo profiles={}", activeProfiles);
            return;
        }

        try {
            LocalDate fechaActual = LocalDate.now(ZONA_LIMA);
            LocalTime horaActual = LocalTime.now(ZONA_LIMA);
            String diaActual = DIAS_SEMANA.get(fechaActual.getDayOfWeek());
            List<String> horasVentana = construirVentana(horaActual);

            marketingMensajeRepository.findByActivoTrueAndHorasEspecificasIsNotNull().stream()
                    .filter(this::tieneCanalConDestinatarios)
                    .forEach(mensaje -> procesarSiCorresponde(
                            mensaje, fechaActual, horasVentana, diaActual));

        } catch (Exception e) {
            log.error("[MarketingScheduler] Error verificando mensajes: {}", e.getMessage(), e);
        }
    }

    private List<String> construirVentana(LocalTime horaActual) {
        List<String> horas = new ArrayList<>(VENTANA_MINUTOS);
        for (int i = 0; i < VENTANA_MINUTOS; i++) {
            horas.add(horaActual.minusMinutes(i).format(HORA_FORMATTER));
        }
        return horas;
    }

    private boolean tieneCanalConDestinatarios(MarketingMensaje mensaje) {
        boolean whatsapp = Boolean.TRUE.equals(mensaje.getWhatsapp())
                && tieneTexto(mensaje.getGrupos());
        boolean fleet = Boolean.TRUE.equals(mensaje.getYandex())
                && tieneTexto(mensaje.getFlotas());
        return whatsapp || fleet;
    }

    private void procesarSiCorresponde(
            MarketingMensaje mensaje,
            LocalDate fechaActual,
            List<String> horasVentana,
            String diaActual) {
        try {
            Map<String, List<String>> horasPorDia = objectMapper.readValue(
                    mensaje.getHorasEspecificas(),
                    new TypeReference<Map<String, List<String>>>() {});

            for (String hora : horasVentana) {
                List<String> dias = horasPorDia.get(hora);
                if (dias == null || dias.stream().noneMatch(
                        dia -> normalizarDia(dia).equals(normalizarDia(diaActual)))) {
                    continue;
                }

                Instant scheduledFor = ZonedDateTime.of(
                        fechaActual,
                        LocalTime.parse(hora, HORA_FORMATTER),
                        ZONA_LIMA).toInstant();
                enviarPorCanales(mensaje, scheduledFor);
                break;
            }
        } catch (Exception e) {
            log.error("[MarketingScheduler] Programación inválida mensajeId={}: {}",
                    mensaje.getId(), e.getMessage());
        }
    }

    private void enviarPorCanales(MarketingMensaje mensaje, Instant scheduledFor) {
        log.info("[MarketingScheduler] Inicio mensajeId={} titulo={}",
                mensaje.getId(), mensaje.getTitulo());

        List<String> grupos = parseJsonList(mensaje.getGrupos());
        List<String> flotas = parseJsonList(mensaje.getFlotas());
        MarketingDeliveryResult whatsapp = MarketingDeliveryResult.vacio();
        MarketingDeliveryResult fleet = MarketingDeliveryResult.vacio();

        if (Boolean.TRUE.equals(mensaje.getWhatsapp())) {
            whatsapp = enviarWhatsAppSeguro(mensaje, grupos, scheduledFor);
        }
        if (Boolean.TRUE.equals(mensaje.getYandex())) {
            fleet = enviarFleetSeguro(mensaje, flotas, scheduledFor);
        }

        log.info("[MarketingScheduler] Fin mensajeId={} whatsapp={}/{} fleet={}/{}",
                mensaje.getId(),
                whatsapp.enviados(), whatsapp.total(),
                fleet.enviados(), fleet.total());
    }

    private MarketingDeliveryResult enviarWhatsAppSeguro(
            MarketingMensaje mensaje, List<String> grupos, Instant scheduledFor) {
        try {
            return whatsAppSender.enviar(mensaje, grupos, scheduledFor);
        } catch (Exception e) {
            log.error(
                    "[MarketingScheduler] Canal WhatsApp interrumpido mensajeId={}: {}",
                    mensaje.getId(),
                    e.getMessage(),
                    e);
            return new MarketingDeliveryResult(0, grupos.size(), 0, grupos.size());
        }
    }

    private MarketingDeliveryResult enviarFleetSeguro(
            MarketingMensaje mensaje, List<String> flotas, Instant scheduledFor) {
        try {
            return fleetSender.enviar(mensaje, flotas, scheduledFor);
        } catch (Exception e) {
            log.error(
                    "[MarketingScheduler] Canal Fleet interrumpido mensajeId={}: {}",
                    mensaje.getId(),
                    e.getMessage(),
                    e);
            return new MarketingDeliveryResult(0, flotas.size(), 0, flotas.size());
        }
    }

    private List<String> parseJsonList(String json) {
        if (!tieneTexto(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("[MarketingScheduler] Lista de destinatarios inválida: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String normalizarDia(String dia) {
        if (dia == null) {
            return "";
        }
        return DIAS_ALTERNATIVOS.getOrDefault(dia, dia);
    }

    private static boolean tieneTexto(String value) {
        return value != null && !value.isBlank();
    }
}
