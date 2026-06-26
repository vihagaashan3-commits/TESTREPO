package com.roadrescue.controller;

import com.roadrescue.dto.OtpDTO;
import com.roadrescue.dto.RegisterDTO;
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

}