package com.roadrescue.controller;

import com.roadrescue.entity.User;
import com.roadrescue.entity.Vehicle;
import com.roadrescue.service.UserService;
import com.roadrescue.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final VehicleService vehicleService;

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<Vehicle> vehicles = vehicleService.getUserVehicles(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("vehicles", vehicles);
        return "user/profile";
    }
}
