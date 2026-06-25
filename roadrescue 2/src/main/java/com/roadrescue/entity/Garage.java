package com.roadrescue.entity;

import com.roadrescue.enums.ServiceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "garages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Garage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Garage name is required")
    @Size(min = 2, max = 100)
    @Column(name = "garage_name", nullable = false)
    private String garageName;

    @NotBlank(message = "Address is required")
    @Column(nullable = false)
    private String address;

    @NotBlank(message = "Phone is required")
    @Column(nullable = false)
    private String phone;

    @Email
    @Column(name = "email")
    private String email;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    @Column(name = "latitude")
    private Double latitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "opening_time")
    private String openingTime;

    @Column(name = "closing_time")
    private String closingTime;

    @Column(name = "is_available")
    private boolean available = true;

    @Column(name = "garage_image")
    private String garageImage;

    @Column(name = "is_deleted")
    private boolean deleted = false;

    @Column(name = "is_verified")
    private boolean verified = false;

    @ElementCollection(targetClass = ServiceType.class)
    @CollectionTable(name = "garage_services", joinColumns = @JoinColumn(name = "garage_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private List<ServiceType> services;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Relationships
    @OneToMany(mappedBy = "garage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Technician> technicians;

    @OneToMany(mappedBy = "garage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BreakdownRequest> breakdownRequests;

    @OneToMany(mappedBy = "garage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;
}
