package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Configuration;
import com.yego.backend.handler.yego_principal.UserNotificationHandler;
import com.yego.backend.repository.yego_principal.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemVersionService {

    private static final String VERSION_KEY = "system_version";
    private static final long BROADCAST_DELAY_SECONDS = 5;

    private final ConfigurationRepository configurationRepository;
    private final UserNotificationHandler userNotificationHandler;

    @Value("${app.system-version:}")
    private String currentVersion;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void verificarVersionEnInicio() {
        if (currentVersion == null || currentVersion.isBlank()) {
            log.info("[SystemVersion] No se ha configurado SYSTEM_VERSION - omitiendo verificación");
            return;
        }

        log.info("[SystemVersion] Versión actual de la aplicación: {}", currentVersion);

        String storedVersion = configurationRepository.findByKey(VERSION_KEY)
                .map(Configuration::getValue)
                .orElse(null);

        if (!currentVersion.equals(storedVersion)) {
            log.info("[SystemVersion] Nueva versión detectada: {} -> {}", storedVersion, currentVersion);

            guardarVersion(currentVersion);

            CompletableFuture.runAsync(() -> {
                try {
                    log.info("[SystemVersion] Esperando {}s para que los clientes WebSocket se reconecten...", BROADCAST_DELAY_SECONDS);
                    TimeUnit.SECONDS.sleep(BROADCAST_DELAY_SECONDS);
                    userNotificationHandler.enviarNuevaVersionDisponible(currentVersion);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.warn("[SystemVersion] No se pudo enviar notificación WebSocket: {}", e.getMessage());
                }
            });
        } else {
            log.info("[SystemVersion] Misma versión que la almacenada ({}), sin cambios", currentVersion);
        }
    }

    private void guardarVersion(String version) {
        configurationRepository.findByKey(VERSION_KEY).ifPresentOrElse(
            config -> {
                config.setValue(version);
                config.setUpdatedAt(LocalDateTime.now());
                configurationRepository.save(config);
                log.info("[SystemVersion] Versión actualizada en BD: {}", version);
            },
            () -> {
                Configuration nueva = Configuration.builder()
                        .key(VERSION_KEY)
                        .value(version)
                        .category("system")
                        .type("string")
                        .description("Versión actual del sistema para notificar actualizaciones a usuarios activos")
                        .build();
                configurationRepository.save(nueva);
                log.info("[SystemVersion] Versión registrada en BD: {}", version);
            }
        );
    }
}
