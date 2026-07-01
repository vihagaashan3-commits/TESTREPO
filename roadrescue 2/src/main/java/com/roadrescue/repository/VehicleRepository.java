package com.roadrescue.repository;

import com.roadrescue.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByUserIdAndDeletedFalse(Long userId);
    Page<Vehicle> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    Optional<Vehicle> findByPlateNumberAndDeletedFalse(String plateNumber);
    boolean existsByPlateNumberAndDeletedFalse(String plateNumber);
    boolean existsByChassisNumberAndDeletedFalse(String chassisNumber);
}