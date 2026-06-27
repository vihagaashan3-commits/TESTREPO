package com.roadrescue.controller;

import com.roadrescue.entity.User;
import com.roadrescue.service.UserService;
import com.roadrescue.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final VehicleService vehicleService;

    // ── GET /profile ──────────────────────────────────────────────
    @GetMapping
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("vehicles", vehicleService.getUserVehicles(user.getId()));
        return "user/profile";
    }

    // ── POST /profile/update  (update name + phone) ───────────────
    @PostMapping("/update")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("fullName") String fullName,
            @RequestParam("phone")    String phone,
            RedirectAttributes ra) {

        try {
            User user = userService.findByEmail(userDetails.getUsername());

            // Basic validation
            if (fullName == null || fullName.trim().length() < 2) {
                ra.addFlashAttribute("error", "Full name must be at least 2 characters.");
                return "redirect:/profile";
            }
            if (phone == null || !phone.trim().matches("^[0-9]{10,15}$")) {
                ra.addFlashAttribute("error", "Phone number must be 10–15 digits.");
                return "redirect:/profile";
            }

            User updated = new User();
            updated.setFullName(fullName.trim());
            updated.setPhone(phone.trim());
            userService.updateProfile(user.getId(), updated);

            ra.addFlashAttribute("success", "Profile updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    // ── POST /profile/change-password ─────────────────────────────
    @PostMapping("/change-password")
    public String changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword")     String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes ra) {

        try {
            if (!newPassword.equals(confirmPassword)) {
                ra.addFlashAttribute("error", "New passwords do not match.");
                return "redirect:/profile";
            }
            if (newPassword.length() < 6) {
                ra.addFlashAttribute("error", "New password must be at least 6 characters.");
                return "redirect:/profile";
            }

            User user = userService.findByEmail(userDetails.getUsername());
            userService.changePassword(user.getId(), currentPassword, newPassword);

            ra.addFlashAttribute("success", "Password changed successfully!");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to change password: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    // ── POST /profile/upload-picture ──────────────────────────────
    @PostMapping("/upload-picture")
    public String uploadPicture(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("profileImage") MultipartFile file,
            RedirectAttributes ra) {

        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "Please select an image file.");
            return "redirect:/profile";
        }

        // Only allow image files
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            ra.addFlashAttribute("error", "Only image files are allowed (JPG, PNG, etc.).");
            return "redirect:/profile";
        }

        // Max 5 MB
        if (file.getSize() > 5 * 1024 * 1024) {
            ra.addFlashAttribute("error", "Image must be smaller than 5 MB.");
            return "redirect:/profile";
        }

        try {
            // Save to uploads/profiles/
            String uploadDir = "uploads/profiles/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Unique file name to avoid collisions
            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String fileName = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Save path in DB
            User user = userService.findByEmail(userDetails.getUsername());
            userService.updateProfileImage(user.getId(), "/" + uploadDir + fileName);

            ra.addFlashAttribute("success", "Profile picture updated successfully!");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Failed to upload picture: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    // ── POST /profile/remove-picture ─────────────────────────────
    @PostMapping("/remove-picture")
    public String removePicture(
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            User user = userService.findByEmail(userDetails.getUsername());

            // Delete physical file if it exists
            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                Path filePath = Paths.get(user.getProfileImage().replaceFirst("^/", ""));
                Files.deleteIfExists(filePath);
            }

            userService.updateProfileImage(user.getId(), null);
            ra.addFlashAttribute("success", "Profile picture removed.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to remove picture: " + e.getMessage());
        }
        return "redirect:/profile";
    }
}
