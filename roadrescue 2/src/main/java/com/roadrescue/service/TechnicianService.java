package com.roadrescue.service;

import com.roadrescue.entity.Garage;
import com.roadrescue.entity.Technician;
import com.roadrescue.exception.ResourceNotFoundException;
import com.roadrescue.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TechnicianService {

    private final TechnicianRepository technicianRepository;
    private final GarageService garageService;

    @Transactional
    public Technician addTechnician(Technician technician, Long garageId) {
        Garage garage = garageService.findById(garageId);
        technician.setGarage(garage);
        return technicianRepository.save(technician);
    }



    public List<Technician> getAvailableByGarage(Long garageId) {
        return technicianRepository.findByGarageIdAndAvailableTrueAndDeletedFalse(garageId);
    }

    public Page<Technician> getByGarage(Long garageId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        return technicianRepository.findByGarageIdAndDeletedFalse(garageId, pageable);
    }

    @Transactional
    public Technician update(Long id, Technician updated) {
        Technician tech = findById(id);
        tech.setName(updated.getName());
        tech.setPhone(updated.getPhone());
        tech.setExperienceYears(updated.getExperienceYears());
        tech.setSkills(updated.getSkills());
        return technicianRepository.save(tech);
    }

    @Transactional
    public void toggleAvailability(Long id) {
        Technician tech = findById(id);
        tech.setAvailable(!tech.isAvailable());
        technicianRepository.save(tech);
    }

    @Transactional
    public void softDelete(Long id) {
        Technician tech = findById(id);
        tech.setDeleted(true);
        technicianRepository.save(tech);
    }
}
