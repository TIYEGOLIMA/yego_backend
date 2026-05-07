package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    /** Solo id + nombre (menos columnas que findAllById para armar DTOs de tareas). */
    interface AreaIdNameRow {
        Long getId();

        String getName();
    }

    @Query("SELECT a.id AS id, a.name AS name FROM Area a WHERE a.id IN :ids")
    List<AreaIdNameRow> findIdAndNameByIdIn(@Param("ids") Collection<Long> ids);

    Optional<Area> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT a FROM Area a ORDER BY a.name ASC")
    List<Area> findAllOrderByNameAsc();

    @Query("SELECT a FROM Area a WHERE a.activo = true ORDER BY a.name ASC")
    List<Area> findAllActivas();

    Optional<Area> findByIdAndActivoTrue(Long id);

    List<Area> findByManagerId(Long managerId);

    List<Area> findBySupervisorId(Long supervisorId);

    /** IDs de usuario que ya son responsables de algún área (para excluirlos del combo, salvo el del área en edición). */
    @Query("SELECT DISTINCT a.managerId FROM Area a WHERE a.managerId IS NOT NULL")
    List<Long> findDistinctManagerIds();

    /** Cambia activo a su valor opuesto en un solo UPDATE (evita SELECT + save). */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Area a SET a.activo = CASE WHEN a.activo = true THEN false ELSE true END WHERE a.id = :id")
    int toggleActivoById(@Param("id") Long id);
}
