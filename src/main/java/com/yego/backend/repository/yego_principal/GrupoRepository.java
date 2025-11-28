package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    
    List<Grupo> findByActivoTrue();
    
    Optional<Grupo> findByNombre(String nombre);
    
    boolean existsByNombre(String nombre);
}

