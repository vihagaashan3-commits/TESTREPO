package com.roadrescue.repository;

import com.roadrescue.entity.Technician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {
    List<Technician> findByGarageIdAndDeletedFalse(Long garageId);
    List<Technician> findByGarageIdAndAvailableTrueAndDeletedFalse(Long garageId);
    Page<Technician> findByGarageIdAndDeletedFalse(Long garageId, Pageable pageable);
}
