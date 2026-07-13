package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findByYangoCarIdAndEliminadoFalseOrderByFechaVigenteAsc(String yangoCarId);

    List<VehicleDocument> findByYangoCarIdOrderByCreatedAtAsc(String yangoCarId);

    long countByYangoCarIdAndTipo(String yangoCarId, String tipo);

    @Query("""
            select d.yangoCarId, count(d)
            from VehicleDocument d
            where d.yangoCarId in :yangoCarIds
              and d.eliminado = false
            group by d.yangoCarId
            """)
    List<Object[]> countActivosByYangoCarIds(@Param("yangoCarIds") Collection<String> yangoCarIds);
}
