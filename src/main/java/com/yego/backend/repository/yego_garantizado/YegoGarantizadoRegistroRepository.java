package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YegoGarantizadoRegistroRepository extends JpaRepository<YegoGarantizadoRegistro, Long> {

    Optional<YegoGarantizadoRegistro> findByLicenciaNumeroAndFlota(String licenciaNumero, String flota);

    Optional<YegoGarantizadoRegistro> findByLicenciaNumero(String licenciaNumero);

    List<YegoGarantizadoRegistro> findBySemana(String semana);

    boolean existsByLicenciaNumeroAndFlota(String licenciaNumero, String flota);

    List<YegoGarantizadoRegistro> findByFlota(String flota);
}
