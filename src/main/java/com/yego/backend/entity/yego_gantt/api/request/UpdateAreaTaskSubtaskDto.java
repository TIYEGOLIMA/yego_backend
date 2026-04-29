package com.yego.backend.entity.yego_gantt.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAreaTaskSubtaskDto {

    @Size(max = 500)
    private String title;

    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal weight;

    private Integer sortOrder;

    private Boolean done;
}
