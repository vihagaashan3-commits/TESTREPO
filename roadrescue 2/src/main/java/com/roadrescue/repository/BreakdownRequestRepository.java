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

import java.util.List;

@Repository
public interface BreakdownRequestRepository extends JpaRepository<BreakdownRequest, Long> {

    Page<BreakdownRequest> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    Page<BreakdownRequest> findByGarageIdAndDeletedFalse(Long garageId, Pageable pageable);

    Page<BreakdownRequest> findByStatusAndDeletedFalse(RequestStatus status, Pageable pageable);

    Page<BreakdownRequest> findByDeletedFalse(Pageable pageable);

    List<BreakdownRequest> findByGarageIdAndStatusAndDeletedFalse(Long garageId, RequestStatus status);

    @Query("SELECT r FROM BreakdownRequest r WHERE r.deleted = false AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:serviceType IS NULL OR r.serviceType = :serviceType)")
    Page<BreakdownRequest> findWithFilters(@Param("status") RequestStatus status,
                                           @Param("serviceType") ServiceType serviceType,
                                           Pageable pageable);

    long countByStatusAndDeletedFalse(RequestStatus status);

    long countByDeletedFalse();
}
