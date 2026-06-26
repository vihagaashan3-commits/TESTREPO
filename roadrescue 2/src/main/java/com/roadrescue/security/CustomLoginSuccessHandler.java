package com.roadrescue.security;

import com.roadrescue.entity.User;
import com.roadrescue.enums.Role;
import com.roadrescue.service.EmailService;
import com.roadrescue.service.OtpService;
import com.roadrescue.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final OtpService otpService;
    private final EmailService emailService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String email = authentication.getName();

        User user = userService.findByEmail(email);

        if (user.getRole() == Role.ROLE_ADMIN) {

            String otp = otpService.generateOtp(email);

            emailService.sendOtpEmail(email, otp);

            request.getSession().setAttribute("admin_email", email);

            response.sendRedirect("/auth/verify-otp");

            return;
        }

        response.sendRedirect("/dashboard");
    }
}