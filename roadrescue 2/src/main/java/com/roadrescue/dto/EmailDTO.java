package com.roadrescue.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

}