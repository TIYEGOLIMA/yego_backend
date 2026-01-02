package com.yego.backend.entity.yego_ticketerera.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de response para listar agentes activos en el sistema YEGO Ticketerera
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentesActivosResponse {
    
    private List<QueueAgentResponse> agentes;
}

