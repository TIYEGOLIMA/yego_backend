package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface YegoGarantizadoRepository extends JpaRepository<YegoGarantizado, Long> {
    
    List<YegoGarantizado> findBySemanaAndActivoTrue(String semana);
    
    List<YegoGarantizado> findByFlotaIdAndSemanaAndActivoTrue(String flotaId, String semana);
}
