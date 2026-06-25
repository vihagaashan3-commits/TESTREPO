package com.roadrescue.dto;

import com.roadrescue.enums.ServiceType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BreakdownRequestDTO {

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotBlank(message = "Description is required")
    @Size(max = 500)
    private String description;

    @NotNull(message = "Location is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;

    @NotNull(message = "Location is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;

    private String locationAddress;

    private Long vehicleId;
}
