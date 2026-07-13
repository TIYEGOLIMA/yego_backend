package com.yego.backend.repository.yego_api_externo;

import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
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
}
