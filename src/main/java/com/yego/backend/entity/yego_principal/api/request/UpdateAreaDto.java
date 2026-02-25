package com.yego.backend.entity.yego_principal.api.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAreaDto {

    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    private Long managerId;

    private Boolean activo;
}
