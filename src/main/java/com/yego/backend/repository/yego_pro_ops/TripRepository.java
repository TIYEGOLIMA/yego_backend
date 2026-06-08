package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByShiftSessionId(UUID shiftSessionId);
}
