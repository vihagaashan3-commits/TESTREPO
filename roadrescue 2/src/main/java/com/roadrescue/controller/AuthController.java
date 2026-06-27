package com.roadrescue.controller;

import com.roadrescue.dto.EmailDTO;
import com.roadrescue.dto.OtpDTO;
import com.roadrescue.dto.RegisterDTO;
import com.roadrescue.dto.ResetPasswordDTO;
import com.roadrescue.entity.User;
import com.roadrescue.service.EmailService;
import com.roadrescue.service.OtpService;
import com.roadrescue.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;
    private final OtpService otpService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerDTO") RegisterDTO dto,
            BindingResult result,
            Model model,
            HttpSession session) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {

            if (userService.emailExists(dto.getEmail())) {
                model.addAttribute("error", "Email already exists.");
                return "auth/register";
            }

            if (!dto.getPassword().equals(dto.getConfirmPassword())) {
                model.addAttribute("error", "Passwords do not match.");
                return "auth/register";
            }

            // Generate OTP
            String otp = otpService.generateOtp(dto.getEmail());

            // Send OTP Email
            emailService.sendOtpEmail(dto.getEmail(), otp);

            // Save registration details temporarily in Session
            session.setAttribute("registerDTO", dto);

            // Go to OTP Page
            return "redirect:/auth/verify-registration-otp";

        } catch (Exception e) {

            model.addAttribute("error", e.getMessage());

            return "auth/register";
        }
    }
    @GetMapping("/verify-otp")
    public String otpPage(Model model) {

        model.addAttribute("otpDTO", new OtpDTO());

        return "auth/verify-otp";
    }

    @GetMapping("/verify-registration-otp")
    public String registrationOtpPage(Model model) {

        model.addAttribute("otpDTO", new OtpDTO());

        return "auth/verify-registration-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @ModelAttribute OtpDTO otpDTO,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("admin_email");

        if (email == null) {
            return "redirect:/auth/login";
        }

        boolean verified =
                otpService.verifyOtp(email, otpDTO.getOtp());

        if (!verified) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Invalid or Expired OTP");

            return "redirect:/auth/verify-otp";
        }

        session.setAttribute("otp_verified", true);

        return "redirect:/admin/dashboard";
    }
    @PostMapping("/verify-registration-otp")
    public String verifyRegistrationOtp(
            @ModelAttribute OtpDTO otpDTO,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        RegisterDTO registerDTO = (RegisterDTO) session.getAttribute("registerDTO");

        if (registerDTO == null) {
            return "redirect:/auth/register";
        }

        boolean verified = otpService.verifyOtp(registerDTO.getEmail(), otpDTO.getOtp());

        if (!verified) {
            redirectAttributes.addFlashAttribute("error", "Invalid or Expired OTP");
            return "redirect:/auth/verify-registration-otp";
        }

        userService.saveUser(registerDTO);

        session.removeAttribute("registerDTO");

        return "redirect:/auth/login?registered";
    }
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {

        model.addAttribute("emailDTO", new EmailDTO());

        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendResetOtp(
            @ModelAttribute EmailDTO emailDTO,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!userService.emailExists(emailDTO.getEmail())) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Email not found.");

            return "redirect:/auth/forgot-password";
        }

        String otp = otpService.generateOtp(emailDTO.getEmail());

        emailService.sendOtpEmail(emailDTO.getEmail(), otp);

        session.setAttribute("reset_email", emailDTO.getEmail());

        return "redirect:/auth/verify-reset-otp";
    }

    @GetMapping("/verify-reset-otp")
    public String verifyResetOtpPage(Model model) {

        model.addAttribute("otpDTO", new OtpDTO());

        return "auth/verify-reset-otp";
    }

    @PostMapping("/verify-reset-otp")
    public String verifyResetOtp(
            @ModelAttribute OtpDTO otpDTO,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("reset_email");

        if (email == null) {

            return "redirect:/auth/forgot-password";
        }

        boolean verified = otpService.verifyOtp(email, otpDTO.getOtp());

        if (!verified) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Invalid or Expired OTP");

            return "redirect:/auth/verify-reset-otp";
        }

        session.setAttribute("reset_verified", true);

        return "redirect:/auth/reset-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(Model model,
                                    HttpSession session) {

        Boolean verified =
                (Boolean) session.getAttribute("reset_verified");

        if (verified == null || !verified) {

            return "redirect:/auth/login";
        }

        model.addAttribute("resetPasswordDTO",
                new ResetPasswordDTO());

        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @ModelAttribute ResetPasswordDTO dto,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Boolean verified =
                (Boolean) session.getAttribute("reset_verified");

        String email =
                (String) session.getAttribute("reset_email");

        if (verified == null || !verified || email == null) {

            return "redirect:/auth/login";
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Passwords do not match.");

            return "redirect:/auth/reset-password";
        }

        userService.resetPassword(email, dto.getPassword());

        session.removeAttribute("reset_verified");
        session.removeAttribute("reset_email");

        return "redirect:/auth/login?resetSuccess";
    }

}