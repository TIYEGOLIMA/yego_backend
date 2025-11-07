package com.yego.backend.repository.yego_premiun;

import com.yego.backend.entity.yego_premiun.entities.DriverMonthlyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverMonthlyStatsRepository extends JpaRepository<DriverMonthlyStats, Long> {
}

