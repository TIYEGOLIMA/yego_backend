package com.yego.backend.entity.yego_pro_ops.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractorSuggestionsRequest {

    @NotBlank(message = "telefono es requerido")
    @JsonProperty("telefono")
    private String telefono;
}
