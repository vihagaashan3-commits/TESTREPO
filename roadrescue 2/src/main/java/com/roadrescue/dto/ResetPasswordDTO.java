package com.roadrescue.dto;

import lombok.Data;

@Data
public class ResetPasswordDTO {

    private String password;

    private String confirmPassword;

}