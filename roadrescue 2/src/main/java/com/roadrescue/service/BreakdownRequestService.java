package com.roadrescue.service;

import com.roadrescue.dto.BreakdownRequestDTO;
import com.roadrescue.entity.*;
import com.roadrescue.enums.RequestStatus;
import com.roadrescue.enums.ServiceType;
import com.roadrescue.exception.ResourceNotFoundException;
import com.roadrescue.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BreakdownRequestService {

    private final BreakdownRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final GarageRepository garageRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    // STEP 1: DRIVER submits request
    // No minimum payment shown or calculated here.
    // The garage will send a quote separately after accepting.
    @Transactional
    public BreakdownRequest createRequest(BreakdownRequestDTO dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ServiceType> selectedServices = dto.getServiceTypes();
        if (selectedServices == null || selectedServices.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one service type.");
        }

        BreakdownRequest request = BreakdownRequest.builder()
                .serviceTypes(selectedServices)
                .description(dto.getDescription())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .locationAddress(dto.getLocationAddress())
                .preferredPaymentMethod(dto.getPreferredPaymentMethod())
                .status(RequestStatus.PENDING)
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

        // Notify garage(s) via WebSocket — never let a notification failure
        // undo the request that was just successfully created/saved above.
        try {
            if (dto.getGarageId() != null) {
                messagingTemplate.convertAndSendToUser(
                        saved.getGarage().getOwner().getEmail(),
                        "/queue/new-request",
                        "New request sent directly to your garage! ID: " + saved.getId());
            } else {
                List<Garage> nearbyGarages = garageRepository.findNearbyGarages(
                        dto.getLatitude(), dto.getLongitude(), 10.0);
                for (Garage garage : nearbyGarages) {
                    messagingTemplate.convertAndSendToUser(
                            garage.getOwner().getEmail(),
                            "/queue/new-request",
                            "New breakdown request nearby! ID: " + saved.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to push WebSocket notification for new request #{}", saved.getId(), e);
        }

        return saved;
    }

    // STEP 2: GARAGE accepts the request (PENDING → ACCEPTED)
    // After accepting, garage will send a quote to the driver.
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

        // ── Side effects (notification + WebSocket push) ──────────────────
        // These must NOT be able to roll back the acceptance above.
        // If either fails, we log it and move on — the driver/garage can
        // still see the updated status next time they load the page.
        try {
            String servicesText = saved.getServiceTypes() != null
                    ? String.join(", ", saved.getServiceTypes().stream()
                    .map(ServiceType::getDisplayName).toList())
                    : "Service";

            notificationService.createNotification(
                    saved.getUser(),
                    "Request Accepted",
                    "Your request for " + servicesText + " has been accepted by " +
                            garage.getGarageName() + ". You will receive a payment quote shortly.",
                    "REQUEST_ACCEPTED", saved);
        } catch (Exception e) {
            log.error("Failed to create acceptance notification for request #{}", requestId, e);
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    saved.getUser().getEmail(), "/queue/request-update", "ACCEPTED:" + garageId);
        } catch (Exception e) {
            log.error("Failed to push WebSocket update for accepted request #{}", requestId, e);
        }

        return saved;
    }

    // STEP 2b: GARAGE declines the request (PENDING → CANCELLED)
    @Transactional
    public BreakdownRequest declineRequest(Long requestId, Long garageId) {
        BreakdownRequest request = findById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request is no longer pending");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setNotes("Declined by garage. Please submit a new request!.");
        BreakdownRequest saved = requestRepository.save(request);

        try {
            notificationService.createNotification(
                    saved.getUser(),
                    "Request Declined",
                    "Your breakdown request was declined. Please submit a new request to another garage.",
                    "REQUEST_DECLINED", saved);
        } catch (Exception e) {
            log.error("Failed to create decline notification for request #{}", requestId, e);
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    saved.getUser().getEmail(), "/queue/request-update", "CANCELLED:" + requestId);
        } catch (Exception e) {
            log.error("Failed to push WebSocket update for declined request #{}", requestId, e);
        }

        return saved;
    }

    // STEP 3: GARAGE sends payment quote to driver (ACCEPTED → QUOTED)
    // Amount is fixed. Message warns driver that amount cannot change once accepted.
    @Transactional
    public BreakdownRequest sendQuote(Long requestId, BigDecimal quoteAmount, String quoteNotes) {
        BreakdownRequest request = findById(requestId);

        if (request.getStatus() != RequestStatus.ACCEPTED) {
            throw new IllegalStateException("Request must be ACCEPTED before sending a quote");
        }

        request.setStatus(RequestStatus.QUOTED);
        request.setQuoteAmount(quoteAmount);
        request.setQuoteNotes(quoteNotes);
        request.setQuotedAt(LocalDateTime.now());
        BreakdownRequest saved = requestRepository.save(request);

        try {
            notificationService.createNotification(
                    saved.getUser(),
                    "Payment Quote Received",
                    "Garage \"" + saved.getGarage().getGarageName() + "\" has sent a payment quote of Rs. " +
                            quoteAmount + ". Details: " + quoteNotes +
                            ". IMPORTANT: This amount is LOCKED and cannot be changed once you approve. " +
                            "Please APPROVE or REJECT the quote.",
                    "QUOTE_RECEIVED", saved);
        } catch (Exception e) {
            log.error("Failed to create quote notification for request #{}", requestId, e);
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    saved.getUser().getEmail(), "/queue/request-update", "QUOTED:" + requestId);
        } catch (Exception e) {
            log.error("Failed to push WebSocket update for quoted request #{}", requestId, e);
        }

        return saved;
    }

    // STEP 4a: DRIVER approves quote (QUOTED → QUOTE_APPROVED)
    // Technician is dispatched. Amount is now fully locked.
    @Transactional
    public BreakdownRequest approveQuote(Long requestId, String userEmail) {
        BreakdownRequest request = findById(requestId);

        if (!request.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Unauthorized");
        }
        if (request.getStatus() != RequestStatus.QUOTED) {
            throw new IllegalStateException("No quote to approve");
        }

        request.setStatus(RequestStatus.QUOTE_APPROVED);
        request.setQuoteApprovedAt(LocalDateTime.now());
        BreakdownRequest saved = requestRepository.save(request);

        try {
            notificationService.createNotification(
                    saved.getGarage().getOwner(),
                    "Quote Approved — Dispatch Technician",
                    "Driver approved the quote of Rs. " + saved.getQuoteAmount() +
                            " for Request #" + requestId + ". Please dispatch your technician now.",
                    "QUOTE_APPROVED", saved);
        } catch (Exception e) {
            log.error("Failed to create quote-approved notification for request #{}", requestId, e);
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    saved.getGarage().getOwner().getEmail(),
                    "/queue/request-update", "QUOTE_APPROVED:" + requestId);
        } catch (Exception e) {
            log.error("Failed to push WebSocket update for quote-approved request #{}", requestId, e);
        }

        return saved;
    }

    // STEP 4b: DRIVER rejects quote (QUOTED → CANCELLED)
    // Driver can now submit a fresh request to a different garage.
    @Transactional
    public BreakdownRequest rejectQuote(Long requestId, String userEmail) {
        BreakdownRequest request = findById(requestId);

        if (!request.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Unauthorized");
        }
        if (request.getStatus() != RequestStatus.QUOTED) {
            throw new IllegalStateException("No quote to reject");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setNotes("Quote rejected by driver. Driver may submit a new request.");
        BreakdownRequest saved = requestRepository.save(request);

        try {
            notificationService.createNotification(
                    saved.getGarage().getOwner(),
                    "Quote Rejected",
                    "Driver rejected the quote for Request #" + requestId + ".",
                    "QUOTE_REJECTED", saved);
        } catch (Exception e) {
            log.error("Failed to create quote-rejected notification for request #{}", requestId, e);
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    saved.getGarage().getOwner().getEmail(),
                    "/queue/request-update", "QUOTE_REJECTED:" + requestId);
        } catch (Exception e) {
            log.error("Failed to push WebSocket update for quote-rejected request #{}", requestId, e);
        }

        return saved;
    }

    // ── STEP 5: GARAGE dispatches technician (QUOTE_APPROVED → IN_PROGRESS)
    @Transactional
    public BreakdownRequest startWork(Long requestId) {
        BreakdownRequest request = findById(requestId);

        if (request.getStatus() != RequestStatus.QUOTE_APPROVED) {
            throw new IllegalStateException("Quote must be approved by driver before starting work");
        }

        request.setStatus(RequestStatus.IN_PROGRESS);
        BreakdownRequest saved = requestRepository.save(request);

        try {
            notificationService.createNotification(
                    saved.getUser(),
                    "Technician On The Way",
                    "Your quote has been approved and our technician is now heading to your location! " +
                            "Locked amount: Rs. " + saved.getQuoteAmount() +
                            ". Technician contact: " + saved.getGarage().getPhone(),
                    "TECHNICIAN_DISPATCHED", saved);
        } catch (Exception e) {
            log.error("Failed to create in-progress notification for request #{}", requestId, e);
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    saved.getUser().getEmail(), "/queue/request-update", "IN_PROGRESS:" + requestId);
        } catch (Exception e) {
            log.error("Failed to push WebSocket update for in-progress request #{}", requestId, e);
        }

        return saved;
    }

    // STEP 6: GARAGE completes the job (IN_PROGRESS → COMPLETED)
    // Final amount = quote amount (already locked — cannot be changed)
    @Transactional
    public BreakdownRequest completeJob(Long requestId) {
        BreakdownRequest request = findById(requestId);

        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new IllegalStateException("Request must be IN_PROGRESS to complete");
        }

        request.setStatus(RequestStatus.COMPLETED);
        request.setFinalAmount(request.getQuoteAmount()); // locked amount
        request.setCompletedAt(LocalDateTime.now());
        BreakdownRequest saved = requestRepository.save(request);

        try {
            notificationService.createNotification(
                    saved.getUser(),
                    "Job Completed",
                    "Work is done! Final amount: Rs. " + saved.getQuoteAmount() +
                            ". Please pay via " + saved.getPreferredPaymentMethod() +
                            ". You can now rate the garage and technician.",
                    "JOB_COMPLETED", saved);
        } catch (Exception e) {
            log.error("Failed to create completion notification for request #{}", requestId, e);
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    saved.getUser().getEmail(), "/queue/request-update", "COMPLETED:" + requestId);
        } catch (Exception e) {
            log.error("Failed to push WebSocket update for completed request #{}", requestId, e);
        }

        return saved;
    }

    // DRIVER: Cancel request
    // Allowed only before QUOTE_APPROVED (once technician dispatched, cannot cancel)
    @Transactional
    public void cancelRequest(Long requestId, String userEmail) {
        BreakdownRequest request = findById(requestId);

        if (!request.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Unauthorized");
        }
        if (request.getStatus() == RequestStatus.QUOTE_APPROVED ||
                request.getStatus() == RequestStatus.IN_PROGRESS ||
                request.getStatus() == RequestStatus.COMPLETED ||
                request.getStatus() == RequestStatus.PAID) {
            throw new IllegalStateException("Cannot cancel at this stage");
        }

        request.setStatus(RequestStatus.CANCELLED);
        requestRepository.save(request);
    }

    // Queries
    public BreakdownRequest findById(Long id) {
        return requestRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    }

    public Page<BreakdownRequest> getRequestsByUser(Long userId, int page, int size) {
        return requestRepository.findByUserIdAndDeletedFalse(
                userId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public Page<BreakdownRequest> getRequestsByGarage(Long garageId, int page, int size) {
        return requestRepository.findByGarageIdAndDeletedFalse(
                garageId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public Page<BreakdownRequest> getAllRequests(int page, int size, RequestStatus status, ServiceType serviceType) {
        return requestRepository.findWithFilters(
                status, serviceType, PageRequest.of(page, size, Sort.by("createdAt").descending()));
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

    public long getTotalCount() { return requestRepository.countByDeletedFalse(); }
    public long getCountByStatus(RequestStatus status) { return requestRepository.countByStatusAndDeletedFalse(status); }
}
