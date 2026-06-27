package com.roadrescue.config;

import com.roadrescue.entity.User;
import com.roadrescue.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final UserService userService;

    // Automatically adds the logged-in user to every page's model,
    // so the navbar can show the profile picture on all pages.
    @ModelAttribute("user")
    public User addUserToModel(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return null;
        return userService.findByEmail(userDetails.getUsername());
    }
}