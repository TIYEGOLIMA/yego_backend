package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findByYangoCarIdAndEliminadoFalseOrderByFechaVigenteAsc(String yangoCarId);

    List<VehicleDocument> findByYangoCarIdOrderByCreatedAtAsc(String yangoCarId);

    long countByYangoCarIdAndTipo(String yangoCarId, String tipo);
}
