package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    
    /**
     * Busca un conductor por teléfono.
     * Solo retorna conductores con work_status = 'working'.
     * Retorna los campos necesarios para PPendientesResponse, priorizando park_id no nulo.
     * @param phone número de teléfono a buscar
     * @return Lista de arrays con los campos necesarios, ordenados por park_id no nulo primero, luego park_id y driver_id
     */
    @Query(value = "SELECT driver_id, park_id, first_name, full_name, phone, license_number, car_id, car_number " +
                   "FROM drivers WHERE phone = :phone AND work_status = 'working' " +
                   "ORDER BY CASE WHEN park_id IS NOT NULL THEN 0 ELSE 1 END, park_id, driver_id " +
                   "LIMIT 10", nativeQuery = true)
    List<Object[]> findAllByPhoneAsDriverApiNative(@Param("phone") String phone);
    
    /**
     * Busca un conductor por licencia.
     * Solo retorna conductores con work_status = 'working'.
     * Retorna los campos necesarios para PPendientesResponse, priorizando park_id no nulo.
     * @param licenseNumber número de licencia a buscar
     * @return Lista de arrays con los campos necesarios, ordenados por park_id no nulo primero, luego park_id y driver_id
     */
    @Query(value = "SELECT driver_id, park_id, first_name, full_name, phone, license_number, car_id, car_number " +
                   "FROM drivers WHERE license_number = :licenseNumber AND work_status = 'working' " +
                   "ORDER BY CASE WHEN park_id IS NOT NULL THEN 0 ELSE 1 END, park_id, driver_id " +
                   "LIMIT 10", nativeQuery = true)
    List<Object[]> findAllByLicenseAsDriverApiNative(@Param("licenseNumber") String licenseNumber);
    
    /**
     * Busca el full_name y car_number de un conductor por su driver_id
     * @param driverId ID del conductor a buscar
     * @return Optional con un array [full_name, car_number] si existe
     */
    @Query(value = "SELECT full_name, car_number FROM drivers WHERE driver_id = :driverId LIMIT 1", nativeQuery = true)
    Optional<Object[]> findFullNameAndCarNumberByDriverId(@Param("driverId") String driverId);
    
    /**
     * Busca un conductor por driver_id
     * Retorna solo los campos necesarios para PPendientesResponse
     * Prioriza los registros con park_id no nulo, ordenando por park_id para consistencia
     * @param driverId ID del conductor a buscar
     * @return Lista de arrays de objetos con los campos necesarios, ordenados por park_id no nulo primero, luego por park_id y driver_id
     */
    @Query(value = "SELECT driver_id, park_id, first_name, full_name, phone, license_number, car_id, car_number " +
                   "FROM drivers WHERE driver_id = :driverId " +
                   "ORDER BY CASE WHEN park_id IS NOT NULL THEN 0 ELSE 1 END, park_id, driver_id " +
                   "LIMIT 10", nativeQuery = true)
    List<Object[]> findAllByDriverIdAsDriverApiNative(@Param("driverId") String driverId);
}

