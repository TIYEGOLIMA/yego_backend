package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para la entidad Option del sistema YEGO Ticketerera
 */
@Repository
public interface OptionRepository extends JpaRepository<Option, Long> {
    
    List<Option> findByActiveTrueOrderByPriorityAsc();
    
    List<Option> findByParentIdIsNullAndActiveTrueOrderByPriorityAsc();
    
    List<Option> findByParentIdAndActiveTrueOrderByPriorityAsc(Long parentId);
}
