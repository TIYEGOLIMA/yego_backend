package com.yego.backend.repository.yego_api_externo;

import com.yego.backend.entity.yego_api_externo.entities.FleetCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FleetCacheRepository extends JpaRepository<FleetCache, String> {
}
