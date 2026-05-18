package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.PaymentPercentage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentPercentageRepository extends JpaRepository<PaymentPercentage, Long> {

    @Query("SELECT p FROM PaymentPercentage p WHERE p.effectiveFrom <= :fecha ORDER BY p.minValidatedTrips DESC")
    List<PaymentPercentage> findApplicableForDate(@Param("fecha") LocalDate fecha);

    Optional<PaymentPercentage> findFirstByMinValidatedTripsAndEffectiveFrom(Integer minValidatedTrips, LocalDate effectiveFrom);
}
