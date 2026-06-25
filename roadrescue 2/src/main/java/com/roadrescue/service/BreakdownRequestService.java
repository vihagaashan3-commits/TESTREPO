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

        BreakdownRequest request = BreakdownRequest.builder()
                .serviceType(dto.getServiceType())
                .description(dto.getDescription())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .locationAddress(dto.getLocationAddress())
                .status(RequestStatus.PENDING)
                .user(user)
                .build();

        if (dto.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
            request.setVehicle(vehicle);
        }

        BreakdownRequest saved = requestRepository.save(request);

        // Notify nearby garage owners via WebSocket
        List<Garage> nearbyGarages = garageRepository.findNearbyGarages(
                dto.getLatitude(), dto.getLongitude(), 10.0);
        for (Garage garage : nearbyGarages) {
            messagingTemplate.convertAndSendToUser(
                    garage.getOwner().getEmail(),
                    "/queue/new-request",
                    "New breakdown request nearby! ID: " + saved.getId()
            );
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

        // Notify user
        notificationService.createNotification(
                request.getUser(),
                "Request Accepted",
                "Your breakdown request has been accepted by " + garage.getGarageName(),
                "REQUEST_ACCEPTED",
                request
        );

        // Real-time update to user
        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(),
                "/queue/request-update",
                "ACCEPTED:" + garageId
        );

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

        // Real-time WebSocket update
        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(),
                "/queue/request-update",
                status.name() + ":" + requestId
        );

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
