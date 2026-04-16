package com.yego.backend.repository.yego_marketing_mensajes;

import com.yego.backend.entity.yego_marketing_mensajes.entities.ModuleMarketingGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleMarketingGroupRepository extends JpaRepository<ModuleMarketingGroup, Long> {

    Optional<ModuleMarketingGroup> findByGroupId(String groupId);

    List<ModuleMarketingGroup> findByGroupIdIn(List<String> groupIds);
}
