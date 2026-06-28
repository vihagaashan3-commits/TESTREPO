package com.roadrescue.repository;

import com.roadrescue.entity.BreakdownRequest;
import com.roadrescue.enums.RequestStatus;
import com.roadrescue.enums.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BreakdownRequestRepository extends JpaRepository<BreakdownRequest, Long> {

    Page<BreakdownRequest> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    Page<BreakdownRequest> findByGarageIdAndDeletedFalse(Long garageId, Pageable pageable);

    @Query("SELECT DISTINCT r FROM BreakdownRequest r WHERE r.deleted = false " +
            "AND (:status IS NULL OR r.status = :status)")
    Page<BreakdownRequest> findWithFilters(
            @Param("status") RequestStatus status,
            @Param("serviceType") ServiceType serviceType,
            Pageable pageable);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(RequestStatus status);
}