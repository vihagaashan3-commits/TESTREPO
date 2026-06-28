package com.roadrescue.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#^()_+=-])[A-Za-z\\d@$!%*?&.#^()_+=-]{8,}$",
            message = "Password must be at least 8 characters and include uppercase, lowercase, number, and special character."
    )
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    private String role; // USER, GARAGE_OWNER, ADMIN

    private String adminCode; //  ADMIN
}