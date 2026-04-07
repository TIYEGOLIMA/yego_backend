package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByActivoTrueOrderByNameAsc();
}
