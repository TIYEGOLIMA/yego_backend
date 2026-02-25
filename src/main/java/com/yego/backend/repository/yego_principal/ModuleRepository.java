package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByActivoTrueOrderByNombreAsc();

    List<Module> findAllByOrderByNombreAsc();
}
