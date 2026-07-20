package com.yego.backend.entity.yego_pro_ops.api.response.mobile;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdminDriverHistoryResponse {
    AdminDriverResponse driver;
    List<MobileShiftResponse> shifts;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean hasMore;
}
