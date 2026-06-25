package com.roadrescue.controller;

import com.roadrescue.entity.*;
import com.roadrescue.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final UserService userService;

    @GetMapping
    public String listVehicles(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<Vehicle> vehicles = vehicleService.getUserVehicles(user.getId());
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("user", user);
        return "vehicle/list";
    }

    @GetMapping("/new")
    public String newVehicleForm(Model model) {
        model.addAttribute("vehicle", new Vehicle());
        return "vehicle/create";
    }

    @PostMapping("/new")
    public String addVehicle(@Valid @ModelAttribute("vehicle") Vehicle vehicle,
                             BindingResult result,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "vehicle/create";
        try {
            vehicleService.addVehicle(vehicle, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "Vehicle added successfully!");
            return "redirect:/vehicles";
        } catch (IllegalArgumentException e) {
            result.rejectValue("plateNumber", "error.plateNumber", e.getMessage());
            return "vehicle/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editVehicleForm(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", vehicleService.findById(id));
        return "vehicle/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateVehicle(@PathVariable Long id,
                                @Valid @ModelAttribute("vehicle") Vehicle vehicle,
                                BindingResult result,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "vehicle/edit";
        vehicleService.updateVehicle(id, vehicle, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Vehicle updated!");
        return "redirect:/vehicles";
    }

    @PostMapping("/{id}/delete")
    public String deleteVehicle(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        vehicleService.softDelete(id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Vehicle removed.");
        return "redirect:/vehicles";
    }
}
