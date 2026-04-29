package com.yego.backend.entity.yego_gantt.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkspaceDto {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 40)
    private String iconKey;

    @Size(max = 2000)
    private String description;

    private List<Long> memberUserIds;
}
