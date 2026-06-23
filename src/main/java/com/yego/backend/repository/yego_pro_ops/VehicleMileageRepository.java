package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.VehicleMileage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleMileageRepository extends JpaRepository<VehicleMileage, Long> {

    List<VehicleMileage> findByYangoCarIdOrderByFechaAsc(String yangoCarId);
}
