package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    /**
     * Obtiene registros simplificados con solo los campos necesarios
     * @param semana semana a buscar
     * @return Lista de objetos con datos simplificados: licencia, fecha_registro, flota, semana
     */
    @Query(value = "SELECT r.yeg_licencia_numero, CAST(r.yeg_fecha_registro AS TIMESTAMP) as yeg_fecha_registro, r.yeg_flota, r.yeg_semana " +
                   "FROM garantizado_registro r " +
                   "WHERE r.yeg_semana = :semana " +
                   "ORDER BY r.yeg_fecha_registro DESC", nativeQuery = true)
    List<Object[]> findRegistrosCompletosBySemana(@Param("semana") String semana);
}
