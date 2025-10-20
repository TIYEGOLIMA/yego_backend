package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    
    Optional<Module> findByNombre(String nombre);
    List<Module> findByActivoTrue();
    List<Module> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);
}
