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
    public String register(@Valid @ModelAttribute("registerDTO") RegisterDTO dto,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            User user = userService.register(dto);
            emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            // ✅ Show error on form (covers wrong admin code, duplicate email, etc.)
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
    @GetMapping("/verify-otp")
    public String otpPage(Model model) {

        model.addAttribute("otpDTO", new OtpDTO());

        return "auth/verify-otp";
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
}