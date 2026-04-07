package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
