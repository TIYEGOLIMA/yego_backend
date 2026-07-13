package com.yego.backend.scheduler.yego_marketing_mensajes;

import com.yego.backend.entity.yego_marketing_mensajes.entities.ModuleMarketingGroup;
import com.yego.backend.repository.yego_marketing_mensajes.ModuleMarketingGroupRepository;
import com.yego.backend.service.yego_marketing_mensajes.MarketingMensajeService;
import com.yego.backend.service.yego_marketing_mensajes.sender.MarketingWhatsAppSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WhatsAppGroupSyncScheduler {

    private final MarketingWhatsAppSender whatsAppSender;
    private final ModuleMarketingGroupRepository groupRepository;
    private final MarketingMensajeService marketingMensajeService;

    public WhatsAppGroupSyncScheduler(MarketingWhatsAppSender whatsAppSender,
                                       ModuleMarketingGroupRepository groupRepository,
                                       MarketingMensajeService marketingMensajeService) {
        this.whatsAppSender = whatsAppSender;
        this.groupRepository = groupRepository;
        this.marketingMensajeService = marketingMensajeService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarAlIniciar() {
        log.info("🔄 [GroupSync] Sincronización inicial de grupos al arrancar la app");
        sincronizarGrupos();
    }

    @Transactional
    public void sincronizarGrupos() {
        try {
            List<Map<String, Object>> gruposApi = whatsAppSender.obtenerGrupos();

            if (gruposApi.isEmpty()) {
                log.warn("⚠️ [GroupSync] No se obtuvieron grupos desde la API, omitiendo sincronización");
                return;
            }

            Set<String> idsDesdeApi = new HashSet<>();
            int actualizados = 0;
            int nuevos = 0;

            for (Map<String, Object> grupoData : gruposApi) {
                String groupId = (String) grupoData.get("id");
                if (groupId == null || groupId.isEmpty()) continue;

                idsDesdeApi.add(groupId);

                Optional<ModuleMarketingGroup> existente = groupRepository.findByGroupId(groupId);

                ModuleMarketingGroup grupo = existente.orElseGet(ModuleMarketingGroup::new);
                if (existente.isEmpty()) {
                    grupo.setGroupId(groupId);
                    nuevos++;
                } else {
                    actualizados++;
                }

                grupo.setSubject((String) grupoData.get("subject"));
                grupo.setSubjectOwner((String) grupoData.get("subjectOwner"));
                grupo.setSubjectTime(toLong(grupoData.get("subjectTime")));
                grupo.setSize(toInteger(grupoData.get("size")));
                grupo.setCreation(toLong(grupoData.get("creation")));
                grupo.setOwner((String) grupoData.get("owner"));
                grupo.setDescription((String) grupoData.get("desc"));
                grupo.setDescriptionId((String) grupoData.get("descId"));
                grupo.setRestrict(toBoolean(grupoData.get("restrict")));
                grupo.setAnnounce(toBoolean(grupoData.get("announce")));
                grupo.setIsCommunity(toBoolean(grupoData.get("isCommunity")));
                grupo.setIsCommunityAnnounce(toBoolean(grupoData.get("isCommunityAnnounce")));

                groupRepository.save(grupo);
            }

            List<ModuleMarketingGroup> paraEliminar = groupRepository.findAll().stream()
                    .filter(g -> !idsDesdeApi.contains(g.getGroupId()))
                    .collect(Collectors.toList());

            int eliminados = paraEliminar.size();
            if (!paraEliminar.isEmpty()) {
                groupRepository.deleteAll(paraEliminar);
            }

            marketingMensajeService.invalidarCacheGrupos();

            log.info("✅ [GroupSync] Sincronización completada - {} nuevos, {} actualizados, {} eliminados (total API: {})",
                    nuevos, actualizados, eliminados, gruposApi.size());

        } catch (Exception e) {
            log.error("❌ [GroupSync] Error en sincronización de grupos: {}", e.getMessage(), e);
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Boolean toBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
}
