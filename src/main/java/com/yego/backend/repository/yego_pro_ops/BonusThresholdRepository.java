package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.BonusThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BonusThresholdRepository extends JpaRepository<BonusThreshold, Long> {

    @Query("SELECT b FROM BonusThreshold b WHERE b.effectiveFrom <= :fecha ORDER BY b.minTrips DESC")
    List<BonusThreshold> findApplicableForDate(@Param("fecha") LocalDate fecha);

    Optional<BonusThreshold> findFirstByMinTripsAndEffectiveFrom(Integer minTrips, LocalDate effectiveFrom);
}
