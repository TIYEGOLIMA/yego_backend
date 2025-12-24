package com.yego.backend.repository.yego_premiun;

import com.yego.backend.entity.yego_premiun.entities.DriverActiveList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverActiveListRepository extends JpaRepository<DriverActiveList, Long> {

    Optional<DriverActiveList> findByDriverId(String driverId);
    
    List<DriverActiveList> findByDriverIdIn(List<String> driverIds);

}


