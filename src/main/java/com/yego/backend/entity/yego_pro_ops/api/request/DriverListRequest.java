package com.yego.backend.entity.yego_pro_ops.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverListRequest {

    @JsonProperty("filter")
    private Map<String, Object> filter;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("projection")
    private List<String> projection;
}

