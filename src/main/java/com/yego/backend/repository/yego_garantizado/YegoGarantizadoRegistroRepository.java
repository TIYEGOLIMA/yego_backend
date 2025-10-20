package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YegoGarantizadoRegistroRepository extends JpaRepository<Registro, Long> {

    Optional<Registro> findByYegLicenciaNumeroAndYegFlota(String licenciaNumero, String flota);

    Optional<Registro> findByYegLicenciaNumero(String licenciaNumero);

    List<Registro> findByYegSemana(String semana);

    boolean existsByYegLicenciaNumeroAndYegFlota(String licenciaNumero, String flota);

    List<Registro> findByYegFlota(String flota);
}
