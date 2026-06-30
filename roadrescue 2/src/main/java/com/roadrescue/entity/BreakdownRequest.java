package com.roadrescue.entity;

import com.roadrescue.enums.RequestStatus;
import com.roadrescue.enums.ServiceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "breakdown_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BreakdownRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    @CollectionTable(name = "request_service_types", joinColumns = @JoinColumn(name = "request_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private List<ServiceType> serviceTypes;

    @NotBlank(message = "Description is required")
    @Size(max = 500)
    @Column(nullable = false)
    private String description;

    @NotNull(message = "Latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "location_address")
    private String locationAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "notes")
    private String notes;

    // Quote fields (set by garage BEFORE dispatching technician)
    // Flow: PENDING → ACCEPTED → QUOTED → QUOTE_APPROVED → IN_PROGRESS → COMPLETED
    // minimumAmount is NOT shown to user at request time — only the quote is shown
    @Column(name = "quote_amount", precision = 10, scale = 2)
    private BigDecimal quoteAmount;

    @Column(name = "quote_notes", length = 500)
    private String quoteNotes;

    @Column(name = "quoted_at")
    private LocalDateTime quotedAt;

    @Column(name = "quote_approved_at")
    private LocalDateTime quoteApprovedAt;

    // Final amount = quoteAmount (locked once driver approves — cannot change)
    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "preferred_payment_method")
    private String preferredPaymentMethod;

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garage_id")
    private Garage garage;

    @OneToOne(mappedBy = "breakdownRequest", cascade = CascadeType.ALL)
    private Assignment assignment;

    @OneToOne(mappedBy = "breakdownRequest", cascade = CascadeType.ALL)
    private Payment payment;

    @OneToMany(mappedBy = "breakdownRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications;

    // Helper: get first service type for single-service compatibility
    public ServiceType getServiceType() {
        if (serviceTypes != null && !serviceTypes.isEmpty()) return serviceTypes.get(0);
        return null;
    }
}