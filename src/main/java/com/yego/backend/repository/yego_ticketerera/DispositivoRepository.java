package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    List<Dispositivo> findByActiveTrueOrderByNameAsc();
}
