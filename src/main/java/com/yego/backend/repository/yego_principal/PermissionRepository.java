package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para permisos del sistema YEGO Principal
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByName(String name);

    List<Permission> findByActiveTrueOrderByModuleAscActionAsc();
}

