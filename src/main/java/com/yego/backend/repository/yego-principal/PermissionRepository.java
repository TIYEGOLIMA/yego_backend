package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para permisos del sistema YEGO Principal
 * Equivalente a PermissionRepository de TypeORM
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * Buscar permiso por nombre
     */
    Optional<Permission> findByName(String name);
    
    /**
     * Buscar permisos activos ordenados
     */
    List<Permission> findByActiveTrueOrderByResourceAscActionAsc();
    
    /**
     * Buscar permisos por recurso
     */
    List<Permission> findByResourceAndActiveTrueOrderByActionAsc(String resource);
    
    /**
     * Verificar si existe un permiso por nombre
     */
    boolean existsByName(String name);
}
