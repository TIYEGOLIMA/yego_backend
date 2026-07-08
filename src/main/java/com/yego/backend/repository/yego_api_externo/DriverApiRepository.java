package com.yego.backend.repository.yego_api_externo;

import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverApiRepository extends JpaRepository<DriverApi, String> {

    List<DriverApi> findByLicenseNumberIgnoreCaseOrLicenseNormalizedNumberIgnoreCase(
            String licenseNumber,
            String licenseNormalizedNumber
    );
}
