package com.yego.backend.repository.yego_api_externo;

import com.yego.backend.entity.yego_api_externo.entities.YangoApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface YangoApiLogRepository extends JpaRepository<YangoApiLog, Long> {

    @Query("SELECT l FROM YangoApiLog l ORDER BY l.createdAt DESC")
    List<YangoApiLog> findAllOrderByCreatedAtDesc();

    @Query("SELECT l FROM YangoApiLog l WHERE l.createdAt >= :desde ORDER BY l.createdAt DESC")
    List<YangoApiLog> findAllSince(@Param("desde") LocalDateTime desde);

    @Query("SELECT l.ipAddress, COUNT(l) FROM YangoApiLog l WHERE l.createdAt >= :desde GROUP BY l.ipAddress ORDER BY COUNT(l) DESC")
    List<Object[]> countByIpSince(@Param("desde") LocalDateTime desde);

    @Query("SELECT COUNT(l) FROM YangoApiLog l WHERE l.createdAt >= :desde")
    Long countSince(@Param("desde") LocalDateTime desde);
}
