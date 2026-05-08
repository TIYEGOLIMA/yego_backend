package com.yego.backend.service.yego_gantt.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_gantt.api.AreaTaskSubtaskChecklistItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AreaTaskSubtaskChecklistCodec {

    private final ObjectMapper objectMapper;

    public List<AreaTaskSubtaskChecklistItemDto> sanitizeChecklist(List<AreaTaskSubtaskChecklistItemDto> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<AreaTaskSubtaskChecklistItemDto> out = new ArrayList<>();
        int n = 0;
        for (AreaTaskSubtaskChecklistItemDto it : raw) {
            if (it == null) {
                continue;
            }
            String t = it.getText() != null ? it.getText().trim() : "";
            if (t.isEmpty()) {
                continue;
            }
            Boolean d = Boolean.TRUE.equals(it.getDone());
            String id = it.getId() != null && !it.getId().isBlank() ? it.getId().trim()
                    : UUID.randomUUID().toString();
            if (t.length() > 400) {
                t = t.substring(0, 400);
            }
            AreaTaskSubtaskChecklistItemDto one = AreaTaskSubtaskChecklistItemDto.builder()
                    .id(id.length() <= 64 ? id : id.substring(0, 64))
                    .text(t)
                    .done(d)
                    .build();
            out.add(one);
            if (++n >= 80) {
                break;
            }
        }
        return out;
    }

    /** Columna checklist_json: cadena serializada o {@code null} si queda vacía. */
    public String checklistJsonOrNull(List<AreaTaskSubtaskChecklistItemDto> raw) {
        List<AreaTaskSubtaskChecklistItemDto> sanitized = sanitizeChecklist(raw);
        if (sanitized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checklist inválido");
        }
    }
}
