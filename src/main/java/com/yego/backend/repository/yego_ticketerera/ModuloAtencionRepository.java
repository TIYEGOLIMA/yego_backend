package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuloAtencionRepository extends JpaRepository<ModuloAtencion, Long> {

    List<ModuloAtencion> findByIsActiveFalseOrderByNameAsc();

    List<ModuloAtencion> findBySedeIdAndIsActiveFalseOrderByNameAsc(Long sedeId);
}
