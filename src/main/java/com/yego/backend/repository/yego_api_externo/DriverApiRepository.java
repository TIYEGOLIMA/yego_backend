package com.yego.backend.repository.yego_api_externo;

import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DriverApiRepository extends JpaRepository<DriverApi, String> {

    List<DriverApi> findByLicenseNumberIgnoreCaseOrLicenseNormalizedNumberIgnoreCase(
            String licenseNumber,
            String licenseNormalizedNumber
    );

    @Query("""
            SELECT d
            FROM DriverApi d
            WHERE d.parkId = :parkId
              AND (
                    LOWER(d.licenseNumber) = LOWER(:license)
                    OR LOWER(d.licenseNormalizedNumber) = LOWER(:license)
              )
            """)
    List<DriverApi> findByParkIdAndLicense(@Param("parkId") String parkId, @Param("license") String license);

    @Query("""
            SELECT d
            FROM DriverApi d
            WHERE d.parkId = :parkId
              AND (
                    LOWER(COALESCE(d.fullName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(d.firstName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(d.middleName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(d.lastName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(d.licenseNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(d.licenseNormalizedNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(d.documentNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY d.fullName ASC
            """)
    List<DriverApi> searchByPark(
            @Param("parkId") String parkId,
            @Param("query") String query,
            Pageable pageable
    );
}
