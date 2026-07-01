package com.roadrescue.service;

import com.roadrescue.entity.User;
import com.roadrescue.entity.Vehicle;
import com.roadrescue.exception.ResourceNotFoundException;
import com.roadrescue.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserService userService;

    @Transactional
    public Vehicle addVehicle(Vehicle vehicle, String ownerEmail) {
        if (vehicleRepository.existsByPlateNumberAndDeletedFalse(vehicle.getPlateNumber())) {
            throw new IllegalArgumentException("Plate number already registered");
        }
        if (vehicleRepository.existsByChassisNumberAndDeletedFalse(vehicle.getChassisNumber())) {
            throw new IllegalArgumentException("Chassis number already registered");
        }
        User user = userService.findByEmail(ownerEmail);
        vehicle.setUser(user);
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getUserVehicles(Long userId) {
        return vehicleRepository.findByUserIdAndDeletedFalse(userId);
    }

    public Vehicle findById(Long id) {
        return vehicleRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    }

    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle updated, String ownerEmail) {
        Vehicle vehicle = findById(id);
        User owner = userService.findByEmail(ownerEmail);
        if (!vehicle.getUser().getId().equals(owner.getId())) {
            throw new SecurityException("Unauthorized");
        }
        vehicle.setBrand(updated.getBrand());
        vehicle.setModel(updated.getModel());
        vehicle.setVehicleType(updated.getVehicleType());
        vehicle.setYear(updated.getYear());
        vehicle.setColor(updated.getColor());

        // Only overwrite images if a new file was actually uploaded
        if (updated.getFrontImagePath() != null) {
            vehicle.setFrontImagePath(updated.getFrontImagePath());
        }
        if (updated.getBackImagePath() != null) {
            vehicle.setBackImagePath(updated.getBackImagePath());
        }
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void softDelete(Long id, String ownerEmail) {
        Vehicle vehicle = findById(id);
        User owner = userService.findByEmail(ownerEmail);
        if (!vehicle.getUser().getId().equals(owner.getId())) {
            throw new SecurityException("Unauthorized");
        }
        vehicle.setDeleted(true);
        vehicleRepository.save(vehicle);
    }
}