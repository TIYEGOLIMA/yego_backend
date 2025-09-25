package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para roles del sistema YEGO Principal
 * Equivalente a RoleRepository de TypeORM
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * Buscar rol por nombre
     */
    Optional<Role> findByName(String name);
    
    /**
     * Obtener todos los roles ordenados por nombre
     */
    @Query("SELECT r FROM Role r ORDER BY r.name ASC")
    List<Role> findAllOrderByNameAsc();
    
    /**
     * Verificar si existe rol por nombre
     */
    boolean existsByName(String name);
    
    /**
     * Buscar roles por lista de nombres
     */
    @Query("SELECT r FROM Role r WHERE r.name IN :names")
    List<Role> findByNameIn(@Param("names") List<String> names);
    
    /**
     * Buscar roles activos
     */
    @Query("SELECT r FROM Role r WHERE r.activo = true ORDER BY r.name ASC")
    List<Role> findActiveRoles();
}
