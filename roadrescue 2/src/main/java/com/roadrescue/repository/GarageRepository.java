package com.roadrescue.repository;

import com.roadrescue.entity.Garage;
import com.roadrescue.enums.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GarageRepository extends JpaRepository<Garage, Long> {

    Page<Garage> findByDeletedFalse(Pageable pageable);

    Page<Garage> findByDeletedFalseAndVerifiedTrue(Pageable pageable);

    List<Garage> findByOwnerIdAndDeletedFalse(Long ownerId);

    @Query("SELECT DISTINCT g FROM Garage g JOIN g.services s WHERE s = :serviceType AND g.deleted = false AND g.verified = true AND g.available = true")
    List<Garage> findByServiceType(@Param("serviceType") ServiceType serviceType);

    @Query(value = "SELECT g.* FROM garages g WHERE g.is_deleted = false AND g.is_verified = true " +
                   "AND g.is_available = true AND " +
                   "(6371 * acos(cos(radians(:lat)) * cos(radians(g.latitude)) * " +
                   "cos(radians(g.longitude) - radians(:lng)) + " +
                   "sin(radians(:lat)) * sin(radians(g.latitude)))) < :radiusKm " +
                   "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(g.latitude)) * " +
                   "cos(radians(g.longitude) - radians(:lng)) + " +
                   "sin(radians(:lat)) * sin(radians(g.latitude))))",
           nativeQuery = true)
    List<Garage> findNearbyGarages(@Param("lat") Double lat,
                                    @Param("lng") Double lng,
                                    @Param("radiusKm") Double radiusKm);

    @Query("SELECT g FROM Garage g WHERE g.deleted = false AND " +
           "(LOWER(g.garageName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(g.address) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Garage> searchGarages(@Param("keyword") String keyword, Pageable pageable);

    long countByDeletedFalse();
}
