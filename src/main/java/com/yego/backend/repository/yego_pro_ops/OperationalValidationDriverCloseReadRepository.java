package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OperationalValidationDriverCloseReadRepository extends JpaRepository<DriverClose, Long> {

    List<DriverClose> findByShiftSessionIdIn(Collection<UUID> shiftSessionIds);
}
