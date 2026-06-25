package com.roadrescue.service;

import com.roadrescue.dto.GarageDTO;
import com.roadrescue.entity.Garage;
import com.roadrescue.entity.User;
import com.roadrescue.enums.ServiceType;
import com.roadrescue.exception.ResourceNotFoundException;
import com.roadrescue.repository.GarageRepository;
import com.roadrescue.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public Garage createGarage(GarageDTO dto, String ownerEmail) {
        User owner = userService.findByEmail(ownerEmail);

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

    public Garage findById(Long id) {
        return garageRepository.findById(id)
                .filter(g -> !g.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Garage not found"));
    }

    @Cacheable("garages")
    public Page<Garage> getAllGarages(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("garageName").ascending());
        if (keyword != null && !keyword.isBlank()) {
            return garageRepository.searchGarages(keyword, pageable);
        }
        return garageRepository.findByDeletedFalse(pageable);
    }

    public List<Garage> getOwnerGarages(String ownerEmail) {
        User owner = userService.findByEmail(ownerEmail);
        return garageRepository.findByOwnerIdAndDeletedFalse(owner.getId());
    }

    @Transactional
    public Garage updateGarage(Long id, GarageDTO dto, String ownerEmail) {
        Garage garage = findById(id);
        User owner = userService.findByEmail(ownerEmail);

        if (!garage.getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("You don't own this garage");
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

    @Transactional
    public void softDelete(Long id) {
        Garage garage = findById(id);
        garage.setDeleted(true);
        garageRepository.save(garage);
    }

    @Transactional
    public void verifyGarage(Long id) {
        Garage garage = findById(id);
        garage.setVerified(true);
        garageRepository.save(garage);
    }

    @Transactional
    public void toggleAvailability(Long id) {
        Garage garage = findById(id);
        garage.setAvailable(!garage.isAvailable());
        garageRepository.save(garage);
    }

    public List<Garage> findNearby(Double lat, Double lng, Double radius) {
        return garageRepository.findNearbyGarages(lat, lng, radius);
    }

    public List<Garage> findByServiceType(ServiceType serviceType) {
        return garageRepository.findByServiceType(serviceType);
    }

    public Double getAverageRating(Long garageId) {
        return reviewRepository.getAverageRatingByGarageId(garageId);
    }

    public long getTotalCount() { return garageRepository.countByDeletedFalse(); }
}
