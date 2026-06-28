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

    // Multiple service types stored as comma-separated string
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
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "notes")
    private String notes;

    // Minimum total cost (sum of all selected service minimums)
    @Column(name = "minimum_amount", precision = 10, scale = 2)
    private BigDecimal minimumAmount;

    // Final amount set by garage after job
    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;

    // Payment method preferred by driver
    @Column(name = "preferred_payment_method")
    private String preferredPaymentMethod;

    @Column(name = "is_deleted")
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

    // Helper: get first service type for compatibility
    public ServiceType getServiceType() {
        if (serviceTypes != null && !serviceTypes.isEmpty()) return serviceTypes.get(0);
        return null;
    }
}