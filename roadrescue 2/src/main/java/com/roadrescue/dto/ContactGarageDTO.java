package com.roadrescue.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class ContactGarageDTO {
    @NotBlank(message = "Subject is required")
    @Size(max = 150, message = "Subject must be under 150 characters")
    private String subject;
    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must be under 2000 characters")
    private String message;
}