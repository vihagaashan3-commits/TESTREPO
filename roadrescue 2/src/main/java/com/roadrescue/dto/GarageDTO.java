package com.roadrescue.dto;

import com.roadrescue.enums.ServiceType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class GarageDTO {

    @NotBlank(message = "Garage name is required")
    @Size(min = 2, max = 100)
    private String garageName;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Phone is required")
    private String phone;

    @Email
    private String email;

    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;

    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;

    private String openingTime;
    private String closingTime;

    @NotEmpty(message = "At least one service is required")
    private List<ServiceType> services;
}
