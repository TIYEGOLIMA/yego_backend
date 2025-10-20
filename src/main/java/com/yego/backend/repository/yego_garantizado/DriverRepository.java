package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con la tabla drivers
 * Entidad de solo lectura para consultas
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, String> {
    
    /**
     * Busca un conductor por su número de licencia (primera coincidencia)
     * @param licenseNumber número de licencia a buscar
     * @return Optional con el conductor si existe
     */
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    
    /**
     * Busca TODOS los registros de un conductor por licencia
     * Puede tener múltiples registros con diferentes park_id
     * @param licenseNumber número de licencia a buscar
     * @return Lista de todos los registros del conductor
     */
    List<Driver> findAllByLicenseNumber(String licenseNumber);
    
    /**
     * Verifica si existe un conductor con el número de licencia dado
     * @param licenseNumber número de licencia a verificar
     * @return true si existe, false si no
     */
    boolean existsByLicenseNumber(String licenseNumber);
}

