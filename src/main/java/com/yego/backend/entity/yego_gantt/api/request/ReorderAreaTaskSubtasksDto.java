package com.yego.backend.entity.yego_gantt.api.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderAreaTaskSubtasksDto {

    /** Ids en el orden definitivo que deben tener en `sort_order` (0 … n−1); debe incluir todas las subtareas del padre, sin repetir. */
    @NotEmpty
    private List<Long> orderedSubtaskIds;
}
