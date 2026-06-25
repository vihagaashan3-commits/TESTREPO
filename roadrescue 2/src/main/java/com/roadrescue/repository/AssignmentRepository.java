package com.roadrescue.repository;

import com.roadrescue.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Optional<Assignment> findByBreakdownRequestId(Long requestId);
    Page<Assignment> findByTechnicianId(Long technicianId, Pageable pageable);
    Page<Assignment> findByGarageId(Long garageId, Pageable pageable);
}
