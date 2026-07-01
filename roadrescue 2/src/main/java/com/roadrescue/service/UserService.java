package com.roadrescue.service;

import com.roadrescue.dto.RegisterDTO;
import com.roadrescue.entity.User;
import com.roadrescue.enums.Role;
import com.roadrescue.exception.ResourceNotFoundException;
import com.roadrescue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Register ──────────────────────────────────────────────────
    @Transactional
    public User register(RegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        Role role = "GARAGE_OWNER".equalsIgnoreCase(dto.getRole())
                ? Role.ROLE_GARAGE_OWNER : Role.ROLE_USER;

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(role)
                .active(true)
                .deleted(false)
                .build();

        return userRepository.save(user);
    }

    // ── Save user (called by AuthController after OTP) ────────────
    @Transactional
    public User saveUser(RegisterDTO dto) {
        Role role = "GARAGE_OWNER".equalsIgnoreCase(dto.getRole())
                ? Role.ROLE_GARAGE_OWNER : Role.ROLE_USER;

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(role)
                .active(true)
                .deleted(false)
                .build();

        return userRepository.save(user);
    }

    // ── Find by email ─────────────────────────────────────────────
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // ── Find by ID ────────────────────────────────────────────────
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // ── Get all users (paginated + search) ───────────────────────
    public Page<User> getAllUsers(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (keyword != null && !keyword.isBlank()) {
            return userRepository.searchUsers(keyword, pageable);
        }
        return userRepository.findByDeletedFalse(pageable);
    }

    // ── Update profile (name + phone) ─────────────────────────────
    @Transactional
    public User updateProfile(Long id, User updatedUser) {
        User user = findById(id);
        user.setFullName(updatedUser.getFullName());
        user.setPhone(updatedUser.getPhone());
        return userRepository.save(user);
    }

    // ── Update profile image path ─────────────────────────────────
    @Transactional
    public User updateProfileImage(Long id, String imagePath) {
        User user = findById(id);
        user.setProfileImage(imagePath);
        return userRepository.save(user);
    }

    // ── Change password ───────────────────────────────────────────
    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = findById(id);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }



    // ── Soft delete ───────────────────────────────────────────────
    @Transactional
    public void softDelete(Long id) {
        User user = findById(id);
        user.setDeleted(true);
        user.setActive(false);
        userRepository.save(user);
    }

    // ── Toggle active ─────────────────────────────────────────────
    @Transactional
    public void toggleActive(Long id) {
        User user = findById(id);
        user.setActive(!user.isActive());
        userRepository.save(user);
    }
    @Transactional
    public void resetPassword(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);
    }

    // ── Count by role ─────────────────────────────────────────────
    public long countByRole(Role role) {
        return userRepository.countByRoleAndDeletedFalse(role);
    }

    // ── Email exists check ────────────────────────────────────────
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById();
    }
}
