package com.roadrescue.service;

import com.roadrescue.dto.BreakdownRequestDTO;
import com.roadrescue.entity.*;
import com.roadrescue.enums.RequestStatus;
import com.roadrescue.enums.ServiceType;
import com.roadrescue.exception.ResourceNotFoundException;
import com.roadrescue.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BreakdownRequestService {

    private final BreakdownRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final GarageRepository garageRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public BreakdownRequest createRequest(BreakdownRequestDTO dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Calculate total minimum from all selected services
        List<ServiceType> selectedServices = dto.getServiceTypes();
        if (selectedServices == null || selectedServices.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one service type.");
        }

        BigDecimal totalMinimum = selectedServices.stream()
                .map(s -> BigDecimal.valueOf(s.getMinimumCharge()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BreakdownRequest request = BreakdownRequest.builder()
                .serviceTypes(selectedServices)
                .description(dto.getDescription())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .locationAddress(dto.getLocationAddress())
                .status(RequestStatus.PENDING)
                .minimumAmount(totalMinimum)
                .preferredPaymentMethod(dto.getPreferredPaymentMethod())
                .user(user)
                .build();

        if (dto.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
            request.setVehicle(vehicle);
        }

        if (dto.getGarageId() != null) {
            Garage garage = garageRepository.findById(dto.getGarageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Garage not found"));
            request.setGarage(garage);
        }

        BreakdownRequest saved = requestRepository.save(request);

        // Notify garage(s)
        if (dto.getGarageId() != null) {
            messagingTemplate.convertAndSendToUser(
                    saved.getGarage().getOwner().getEmail(),
                    "/queue/new-request",
                    "New request sent directly to your garage! ID: " + saved.getId()
            );
        } else {
            List<Garage> nearbyGarages = garageRepository.findNearbyGarages(
                    dto.getLatitude(), dto.getLongitude(), 10.0);
            for (Garage garage : nearbyGarages) {
                messagingTemplate.convertAndSendToUser(
                        garage.getOwner().getEmail(),
                        "/queue/new-request",
                        "New breakdown request nearby! ID: " + saved.getId()
                );
            }
        }

        return saved;
    }

    @Transactional
    public BreakdownRequest acceptRequest(Long requestId, Long garageId) {
        BreakdownRequest request = findById(requestId);
        Garage garage = garageRepository.findById(garageId)
                .orElseThrow(() -> new ResourceNotFoundException("Garage not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request is no longer pending");
        }

        request.setStatus(RequestStatus.ACCEPTED);
        request.setGarage(garage);
        BreakdownRequest saved = requestRepository.save(request);

        String servicesText = request.getServiceTypes() != null
                ? String.join(", ", request.getServiceTypes().stream()
                                    .map(s -> s.getDisplayName()).toList())
                : "Service";

        notificationService.createNotification(
                request.getUser(),
                "Request Accepted",
                "Your request for " + servicesText + " has been accepted by " +
                        garage.getGarageName() + ". Minimum charge: Rs. " + request.getMinimumAmount() +
                        ". Final price may vary after inspection.",
                "REQUEST_ACCEPTED", request
        );

        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(), "/queue/request-update", "ACCEPTED:" + garageId);

        return saved;
    }

    @Transactional
    public BreakdownRequest declineRequest(Long requestId, Long garageId) {
        BreakdownRequest request = findById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request is no longer pending");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setNotes("Declined by garage. Please submit a new request.");
        BreakdownRequest saved = requestRepository.save(request);

        notificationService.createNotification(
                request.getUser(),
                "Request Declined",
                "Your breakdown request was declined by the garage. Please submit a new request.",
                "REQUEST_DECLINED", request
        );

        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(), "/queue/request-update", "CANCELLED:" + requestId);

        return saved;
    }

    @Transactional
    public BreakdownRequest setFinalAmount(Long requestId, BigDecimal finalAmount) {
        BreakdownRequest request = findById(requestId);
        request.setFinalAmount(finalAmount);
        BreakdownRequest saved = requestRepository.save(request);

        notificationService.createNotification(
                request.getUser(),
                "Final Price Confirmed",
                "The garage has confirmed the final repair cost: Rs. " + finalAmount +
                        ". Preferred payment method: " + request.getPreferredPaymentMethod() +
                        ". Please be ready to pay on completion.",
                "FINAL_AMOUNT_SET", request
        );

        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(), "/queue/request-update", "FINAL_AMOUNT:" + requestId);

        return saved;
    }

    @Transactional
    public BreakdownRequest updateStatus(Long requestId, RequestStatus status) {
        BreakdownRequest request = findById(requestId);
        request.setStatus(status);
        if (status == RequestStatus.COMPLETED) {
            request.setCompletedAt(LocalDateTime.now());
        }
        BreakdownRequest saved = requestRepository.save(request);

        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(),
                "/queue/request-update", status.name() + ":" + requestId);

        return saved;
    }

    public BreakdownRequest findById(Long id) {
        return requestRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    }

    public Page<BreakdownRequest> getRequestsByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return requestRepository.findByUserIdAndDeletedFalse(userId, pageable);
    }

    public Page<BreakdownRequest> getRequestsByGarage(Long garageId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return requestRepository.findByGarageIdAndDeletedFalse(garageId, pageable);
    }

    public Page<BreakdownRequest> getAllRequests(int page, int size, RequestStatus status, ServiceType serviceType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return requestRepository.findWithFilters(status, serviceType, pageable);
    }

    public List<Garage> findNearbyGarages(Double lat, Double lng, ServiceType serviceType) {
        List<Garage> nearby = garageRepository.findNearbyGarages(lat, lng, 15.0);
        if (serviceType != null) {
            return nearby.stream()
                    .filter(g -> g.getServices().contains(serviceType))
                    .toList();
        }
        return nearby;
    }

    @Transactional
    public void cancelRequest(Long requestId, String userEmail) {
        BreakdownRequest request = findById(requestId);
        if (!request.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Unauthorized");
        }
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed request");
        }
        request.setStatus(RequestStatus.CANCELLED);
        requestRepository.save(request);
    }

    public long getTotalCount() { return requestRepository.countByDeletedFalse(); }
    public long getCountByStatus(RequestStatus status) { return requestRepository.countByStatusAndDeletedFalse(status); }
}