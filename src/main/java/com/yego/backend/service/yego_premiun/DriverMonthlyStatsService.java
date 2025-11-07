package com.yego.backend.service.yego_premiun;

import com.yego.backend.entity.yego_premiun.api.response.DriverMonthlyStatsResponse;

import java.util.List;

public interface DriverMonthlyStatsService {

    List<DriverMonthlyStatsResponse> obtenerEstadisticas();
}

