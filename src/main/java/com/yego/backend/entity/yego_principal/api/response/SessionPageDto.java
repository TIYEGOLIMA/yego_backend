package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionPageDto {
    private List<SessionResponseDto> content;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer totalPages;
}
