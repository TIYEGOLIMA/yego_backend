package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para respuesta paginada de usuarios en YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPageDto {
    private List<UserResponseCompleteDto> users;
    private Long total;
    private Integer page;
    private Integer limit;
    private Integer totalPages;
    private String search;
    private Boolean active;
}

