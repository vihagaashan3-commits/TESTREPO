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

    // ── DRIVER: Submit new request ──────────────────────────────────────
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

        // Notify garage(s) via WebSocket
        if (dto.getGarageId() != null) {
            messagingTemplate.convertAndSendToUser(
                    saved.getGarage().getOwner().getEmail(),
                    "/queue/new-request",
                    "New request sent directly to your garage! ID: " + saved.getId());
        } else {
            List<Garage> nearby = garageRepository.findNearbyGarages(
                    dto.getLatitude(), dto.getLongitude(), 10.0);
            for (Garage g : nearby) {
                messagingTemplate.convertAndSendToUser(
                        g.getOwner().getEmail(),
                        "/queue/new-request",
                        "New breakdown request nearby! ID: " + saved.getId());
            }
        }
        return saved;
    }

    // ── GARAGE: Accept request ──────────────────────────────────────────
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
                                    .map(ServiceType::getDisplayName).toList())
                : "Service";

        notificationService.createNotification(request.getUser(),
                "Request Accepted",
                "Your request for " + servicesText + " has been accepted by " +
                        garage.getGarageName() + ". You will receive a quote shortly.",
                "REQUEST_ACCEPTED", request);

        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(), "/queue/request-update", "ACCEPTED:" + garageId);
        return saved;
    }

    // ── GARAGE: Decline request ─────────────────────────────────────────
    @Transactional
    public BreakdownRequest declineRequest(Long requestId, Long garageId) {
        BreakdownRequest request = findById(requestId);
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request is no longer pending");
        }
        request.setStatus(RequestStatus.CANCELLED);
        request.setNotes("Declined by garage. Please submit a new request.");
        BreakdownRequest saved = requestRepository.save(request);

        notificationService.createNotification(request.getUser(),
                "Request Declined",
                "Your breakdown request was declined. Please submit a new request to another garage.",
                "REQUEST_DECLINED", request);

        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(), "/queue/request-update", "CANCELLED:" + requestId);
        return saved;
    }

    // ── GARAGE: Send quote (ACCEPTED → QUOTED) ──────────────────────────
    // Garage sends payment quote BEFORE dispatching technician.
    // Quote amount is locked once driver approves — cannot be changed.
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

        notificationService.createNotification(request.getUser(),
                "Payment Quote Received",
                "Garage " + request.getGarage().getGarageName() +
                        " has sent a payment quote of Rs. " + quoteAmount +
                        ". Details: " + quoteNotes +
                        ". NOTE: This amount is fixed and cannot be changed once approved." +
                        " Please APPROVE or REJECT.",
                "QUOTE_RECEIVED", request);

        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(), "/queue/request-update", "QUOTED:" + requestId);
        return saved;
    }

    // ── DRIVER: Approve quote (QUOTED → QUOTE_APPROVED) ────────────────
    // Once approved, technician is dispatched and amount is locked.
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

        notificationService.createNotification(
                request.getGarage().getOwner(),
                "Quote Approved — Dispatch Technician",
                "Driver approved the quote of Rs. " + request.getQuoteAmount() +
                        " for Request #" + requestId + ". Please dispatch your technician now.",
                "QUOTE_APPROVED", request);

        messagingTemplate.convertAndSendToUser(
                request.getGarage().getOwner().getEmail(),
                "/queue/request-update", "QUOTE_APPROVED:" + requestId);
        return saved;
    }

    // ── DRIVER: Reject quote (QUOTED → CANCELLED) ───────────────────────
    // Driver can reject and submit a fresh request to another garage.
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

        notificationService.createNotification(
                request.getGarage().getOwner(),
                "Quote Rejected",
                "Driver rejected the quote for Request #" + requestId + ".",
                "QUOTE_REJECTED", request);

        messagingTemplate.convertAndSendToUser(
                request.getGarage().getOwner().getEmail(),
                "/queue/request-update", "QUOTE_REJECTED:" + requestId);
        return saved;
    }

    // ── GARAGE: Dispatch technician / start work (QUOTE_APPROVED → IN_PROGRESS) ──
    @Transactional
    public BreakdownRequest startWork(Long requestId) {
        BreakdownRequest request = findById(requestId);
        if (request.getStatus() != RequestStatus.QUOTE_APPROVED) {
            throw new IllegalStateException("Quote must be approved by driver before starting work");
        }
        request.setStatus(RequestStatus.IN_PROGRESS);
        BreakdownRequest saved = requestRepository.save(request);

        notificationService.createNotification(request.getUser(),
                "Technician Dispatched",
                "Your quote has been approved. Our technician is on the way! " +
                        "Locked amount: Rs. " + request.getQuoteAmount() +
                        ". Contact: " + request.getGarage().getPhone(),
                "TECHNICIAN_DISPATCHED", request);

        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(), "/queue/request-update", "IN_PROGRESS:" + requestId);
        return saved;
    }

    // ── GARAGE: Complete job (IN_PROGRESS → COMPLETED) ──────────────────
    @Transactional
    public BreakdownRequest completeJob(Long requestId) {
        BreakdownRequest request = findById(requestId);
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new IllegalStateException("Request must be IN_PROGRESS to complete");
        }
        request.setStatus(RequestStatus.COMPLETED);
        request.setFinalAmount(request.getQuoteAmount()); // Final = approved quote (locked)
        request.setCompletedAt(LocalDateTime.now());
        BreakdownRequest saved = requestRepository.save(request);

        notificationService.createNotification(request.getUser(),
                "Job Completed",
                "Work is done! Final amount: Rs. " + request.getQuoteAmount() +
                        ". Please pay via " + request.getPreferredPaymentMethod() +
                        ". You can now rate the garage and technician.",
                "JOB_COMPLETED", request);

        messagingTemplate.convertAndSendToUser(
                request.getUser().getEmail(), "/queue/request-update", "COMPLETED:" + requestId);
        return saved;
    }

    // ── DRIVER: Cancel (only when PENDING, ACCEPTED, or QUOTED) ─────────
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