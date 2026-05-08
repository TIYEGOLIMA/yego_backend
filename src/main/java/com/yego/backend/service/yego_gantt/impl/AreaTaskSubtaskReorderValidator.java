package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.entity.yego_gantt.entities.AreaTaskSubtask;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validación previa al reordenar subtareas por ids (conjunto igual, sin repetir, mismo tamaño).
 */
final class AreaTaskSubtaskReorderValidator {

    private AreaTaskSubtaskReorderValidator() {}

    static void assertValidReorderPayload(List<Long> orderedSubtaskIds, List<AreaTaskSubtask> existing) {
        if (orderedSubtaskIds == null || orderedSubtaskIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lista de orden vacía");
        }
        if (existing.size() != orderedSubtaskIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El número de subtareas no coincide con el padre (esperado "
                            + existing.size()
                            + ", recibido "
                            + orderedSubtaskIds.size()
                            + ")");
        }
        Set<Long> unique = new HashSet<>(orderedSubtaskIds);
        if (unique.size() != orderedSubtaskIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay ids duplicados en el orden");
        }
        Set<Long> expectedIds =
                existing.stream().map(AreaTaskSubtask::getId).collect(Collectors.toCollection(HashSet::new));
        if (!expectedIds.equals(unique)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Los ids no coinciden exactamente con las subtareas del padre");
        }
    }
}
