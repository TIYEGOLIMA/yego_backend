package com.yego.backend.entity.yego_gantt.api;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ítem de checklist de una subtarea (persistido como JSON). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaTaskSubtaskChecklistItemDto {

    @Size(max = 64)
    private String id;

    @Size(max = 400)
    private String text;

    private Boolean done;
}
