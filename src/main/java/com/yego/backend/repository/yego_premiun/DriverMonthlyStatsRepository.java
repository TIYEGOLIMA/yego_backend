package com.yego.backend.repository.yego_premiun;

import com.yego.backend.entity.yego_premiun.entities.DriverMonthlyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverMonthlyStatsRepository extends JpaRepository<DriverMonthlyStats, Long> {

    List<DriverMonthlyStats> findAllByYearAndMonth(Integer year, Integer month);
    
    List<DriverMonthlyStats> findAllByMonth(Integer month);
}

