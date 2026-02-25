package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    Optional<Area> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT a FROM Area a ORDER BY a.name ASC")
    List<Area> findAllOrderByNameAsc();

    @Query("SELECT a FROM Area a WHERE a.activo = true ORDER BY a.name ASC")
    List<Area> findAllActivas();

    List<Area> findByManagerId(Long managerId);
}
