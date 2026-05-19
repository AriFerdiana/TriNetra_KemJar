package com.smartwaste.repository;

import com.smartwaste.entity.SmartBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmartBinRepository extends JpaRepository<SmartBin, String> {
    Optional<SmartBin> findByDeviceId(String deviceId);
}
