package com.roadrescue.repository;

import com.roadrescue.entity.AdminOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminOtpRepository
        extends JpaRepository<AdminOtp, Long> {

    Optional<AdminOtp> findTopByEmailOrderByIdDesc(String email);
}