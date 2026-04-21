package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    List<Dispositivo> findBySedeIdAndActiveTrueOrderByNameAsc(Long sedeId);

    List<Dispositivo> findByActiveTrueOrderByNameAsc();

    Optional<Dispositivo> findByAccessToken(String accessToken);
}
