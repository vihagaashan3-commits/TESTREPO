package com.roadrescue.controller;

import com.roadrescue.enums.Role;
import com.roadrescue.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final GarageService garageService;

    @GetMapping("/users")
    public String manageUsers(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) String keyword,
                              Model model) {
        model.addAttribute("users", userService.getAllUsers(page, size, keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.toggleActive(id);
        ra.addFlashAttribute("success", "User status updated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.deleteUser(id);
        ra.addFlashAttribute("success", "User deleted.");
        return "redirect:/admin/users";
    }
    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/garages")
    public String manageGarages(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String serviceType,
                                Model model) {

        model.addAttribute("garages",
                garageService.getAllGarages(page, size, keyword, serviceType));

        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("serviceType", serviceType);

        return "admin/garages";
    }

    @PostMapping("/garages/{id}/verify")
    public String verifyGarage(@PathVariable Long id, RedirectAttributes ra) {
        garageService.verifyGarage(id);
        ra.addFlashAttribute("success", "Garage verified!");
        return "redirect:/admin/garages";
    }


    @PostMapping("/garages/{id}/delete")
    public String deleteGarage(@PathVariable Long id, RedirectAttributes ra) {
        garageService.softDelete(id);
        ra.addFlashAttribute("success", "Garage deleted.");
        return "redirect:/admin/garages";
    }
}
