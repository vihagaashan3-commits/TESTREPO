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
@Table(name = "technicians")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid phone number")
    @Column(nullable = false)
    private String phone;

    @Column(name = "experience_years")
    @Min(0) @Max(50)
    private Integer experienceYears;

    @Column(name = "is_available")
    private boolean available = true;

    @Column(name = "is_deleted")
    private boolean deleted = false;

    @Column(name = "profile_image")
    private String profileImage;

    @ElementCollection(targetClass = ServiceType.class)
    @CollectionTable(name = "technician_skills", joinColumns = @JoinColumn(name = "technician_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "skill")
    private List<ServiceType> skills;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "technician", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Assignment> assignments;
}
