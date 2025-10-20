package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para operaciones con la tabla garantizado_registro
 * Maneja la persistencia de registros de conductores garantizados
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Repository
public interface RegistroRepository extends JpaRepository<Registro, Long> {
    
    /**
     * Busca un registro por número de licencia
     * @param yegLicenciaNumero número de licencia a buscar
     * @return Optional con el registro si existe
     */
    Optional<Registro> findByYegLicenciaNumero(String yegLicenciaNumero);
    
    /**
     * Verifica si existe un registro con el número de licencia dado
     * @param yegLicenciaNumero número de licencia a verificar
     * @return true si existe, false si no
     */
    boolean existsByYegLicenciaNumero(String yegLicenciaNumero);
    
    /**
     * Verifica si existe un registro con el número de licencia y semana dados
     * @param yegLicenciaNumero número de licencia a verificar
     * @param yegSemana semana a verificar
     * @return true si existe, false si no
     */
    boolean existsByYegLicenciaNumeroAndYegSemana(String yegLicenciaNumero, String yegSemana);
}

