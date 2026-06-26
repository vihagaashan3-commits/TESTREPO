package com.roadrescue.service;

import com.roadrescue.entity.AdminOtp;
import com.roadrescue.repository.AdminOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final AdminOtpRepository otpRepository;

    public String generateOtp(String email) {

        String otp =
                String.format("%06d",
                        new Random().nextInt(999999));

        AdminOtp adminOtp =
                AdminOtp.builder()
                        .email(email)
                        .otpCode(otp)
                        .expiryTime(LocalDateTime.now().plusMinutes(5))
                        .used(false)
                        .build();

        otpRepository.save(adminOtp);

        return otp;
    }
    public boolean verifyOtp(String email, String otp) {

        AdminOtp adminOtp = otpRepository
                .findTopByEmailOrderByIdDesc(email)
                .orElse(null);

        if (adminOtp == null) {
            return false;
        }

        if (adminOtp.isUsed()) {
            return false;
        }

        if (adminOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        if (!adminOtp.getOtpCode().equals(otp)) {
            return false;
        }

        adminOtp.setUsed(true);
        otpRepository.save(adminOtp);

        return true;
    }
}
