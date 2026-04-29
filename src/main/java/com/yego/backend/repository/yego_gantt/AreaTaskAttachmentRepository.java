package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaTaskAttachmentRepository extends JpaRepository<AreaTaskAttachment, Long> {

    List<AreaTaskAttachment> findByTaskIdOrderByCreatedAtDescIdDesc(Long taskId);
}
