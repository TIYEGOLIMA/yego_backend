package com.yego.backend.service.yego_premiun;

import com.yego.backend.entity.yego_premiun.api.response.DriverTripsMonthResponse;
import com.yego.backend.entity.yego_premiun.api.response.DriverTripsYearResponse;

public interface DriverTripsMonthService {

    DriverTripsMonthResponse listCompletedTripsForMonth(String driverId, int month, int year);

    /** Viajes y Yango Pro agregados por mes en todo el año (tabla trips_{year}). */
    DriverTripsYearResponse listCompletedTripsForYear(String driverId, int year);
}
