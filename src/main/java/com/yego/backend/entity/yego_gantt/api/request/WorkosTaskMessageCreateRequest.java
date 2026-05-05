package com.yego.backend.entity.yego_gantt.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkosTaskMessageCreateRequest {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(min = 1, max = 5000, message = "El mensaje debe tener entre 1 y 5000 caracteres")
    private String content;

    /** Si se informa, debe ser una subtarea de esta tarea */
    private Long subtaskId;
}
