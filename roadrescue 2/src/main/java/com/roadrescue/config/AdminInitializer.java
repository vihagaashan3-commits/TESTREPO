package com.roadrescue.config;

import com.roadrescue.entity.User;
import com.roadrescue.enums.Role;
import com.roadrescue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!userRepository.existsByRole(Role.ROLE_ADMIN)) {

            User admin = User.builder()
                    .fullName("System Administrator")
                    .email("nadeeshakalhara685@gmail.com")
                    .phone("0770000000")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ROLE_ADMIN)
                    .twoFactorEnabled(true)
                    .active(true)
                    .deleted(false)
                    .createdBy("SYSTEM")
                    .build();

            userRepository.save(admin);

            System.out.println("ADMIN ACCOUNT CREATED");
        }
    }
}