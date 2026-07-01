package com.roadrescue.service;

import com.roadrescue.dto.GarageDTO;
import com.roadrescue.entity.Garage;
import com.roadrescue.entity.User;
import com.roadrescue.enums.ServiceType;
import com.roadrescue.exception.DuplicateGarageException;
import com.roadrescue.exception.ResourceNotFoundException;
import com.roadrescue.repository.GarageRepository;
import com.roadrescue.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GarageService {

    private final GarageRepository garageRepository;
    private final ReviewRepository reviewRepository;
    private final UserService userService;

    // CREATE

    @Transactional
    @CacheEvict(value = "garages", allEntries = true)
    public Garage createGarage(GarageDTO dto, String ownerEmail) {

        User owner = userService.findByEmail(ownerEmail);

        if (garageRepository.existsByOwnerIdAndDeletedFalse(owner.getId())) {
            throw new DuplicateGarageException("Only one garage per account allowed.");
        }

        Garage garage = Garage.builder()
                .garageName(dto.getGarageName())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .openingTime(dto.getOpeningTime())
                .closingTime(dto.getClosingTime())
                .services(dto.getServices())
                .owner(owner)
                .verified(false)
                .available(true)
                .build();

        return garageRepository.save(garage);
    }

    // ================= FIND BY ID =================

    public Garage findById(Long id) {
        return garageRepository.findById(id)
                .filter(g -> !g.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Garage not found"));
    }

    // ================= MAIN LIST =================

    @Cacheable(value = "garages", key = "#page + '-' + #size + '-' + #keyword + '-' + #serviceType")
    public Page<Garage> getAllGarages(int page, int size, String keyword, String serviceType) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("garageName").ascending());

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasService = serviceType != null && !serviceType.isBlank();

        if (hasKeyword && hasService) {
            return garageRepository.searchByKeywordAndService(
                    keyword,
                    ServiceType.valueOf(serviceType),
                    pageable
            );
        }

        if (hasService) {
            return garageRepository.findByServiceType(
                    ServiceType.valueOf(serviceType),
                    pageable
            );
        }

        if (hasKeyword) {
            return garageRepository.searchGarages(keyword, pageable);
        }

        return garageRepository.findByDeletedFalse(pageable);
    }

    // ================= NEARBY =================

    public List<Garage> findNearbyGarages(Double lat, Double lng, Double radiusKm) {
        return garageRepository.findNearbyGarages(lat, lng, radiusKm);
    }

    // ================= OWNER =================

    public List<Garage> getOwnerGarages(String email) {
        User owner = userService.findByEmail(email);
        return garageRepository.findByOwnerIdAndDeletedFalse(owner.getId());
    }

    public boolean ownerHasGarage(String email) {
        User owner = userService.findByEmail(email);
        return garageRepository.existsByOwnerIdAndDeletedFalse(owner.getId());
    }

    // ================= UPDATE =================

    @Transactional
    @CacheEvict(value = "garages", allEntries = true)
    public Garage updateGarage(Long id, GarageDTO dto, String email) {

        Garage garage = findById(id);
        User owner = userService.findByEmail(email);

        if (!garage.getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("Not your garage");
        }

        garage.setGarageName(dto.getGarageName());
        garage.setAddress(dto.getAddress());
        garage.setPhone(dto.getPhone());
        garage.setEmail(dto.getEmail());
        garage.setLatitude(dto.getLatitude());
        garage.setLongitude(dto.getLongitude());
        garage.setOpeningTime(dto.getOpeningTime());
        garage.setClosingTime(dto.getClosingTime());
        garage.setServices(dto.getServices());

        return garageRepository.save(garage);
    }

    // ================= SOFT DELETE =================

    @Transactional
    @CacheEvict(value = "garages", allEntries = true)
    public void softDelete(Long id) {
        Garage garage = findById(id);
        garage.setDeleted(true);
        garageRepository.save(garage);
    }

    // ================= VERIFY =================

    @Transactional
    @CacheEvict(value = "garages", allEntries = true)
    public void verifyGarage(Long id) {
        Garage garage = findById(id);
        garage.setVerified(true);
        garageRepository.save(garage);
    }

    // ================= TOGGLE =================

    @Transactional
    @CacheEvict(value = "garages", allEntries = true)
    public void toggleAvailability(Long id) {
        Garage garage = findById(id);
        garage.setAvailable(!garage.isAvailable());
        garageRepository.save(garage);
    }

    // ================= REVIEWS =================

    public Double getAverageRating(Long garageId) {
        return reviewRepository.getAverageRatingByGarageId(garageId);
    }

    public long getTotalCount() {
        return garageRepository.countByDeletedFalse();
    }
}