package com.roadrescue.controller;
import com.roadrescue.dto.ContactCustomerDTO;
import com.roadrescue.dto.ContactGarageDTO;
import com.roadrescue.entity.User;
import com.roadrescue.service.EmailService;
import com.roadrescue.service.UserService;
import com.roadrescue.dto.GarageDTO;
import com.roadrescue.entity.Garage;
import com.roadrescue.enums.ServiceType;
import com.roadrescue.service.GarageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/garages")
@RequiredArgsConstructor
public class GarageController {

    private final GarageService garageService;
    private final UserService userService;
    private final EmailService emailService;

    @GetMapping
    public String listGarages(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "9") int size,
                              @RequestParam(required = false) String keyword,
                              Model model) {
        Page<Garage> garages = garageService.getAllGarages(page, size, keyword);
        model.addAttribute("garages", garages);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("serviceTypes", ServiceType.values());
        return "garage/list";
    }

    @GetMapping("/{id}")
    public String viewGarage(@PathVariable Long id,
                             Model model,
                             @AuthenticationPrincipal UserDetails userDetails) {
        Garage garage = garageService.findById(id);
        Double avgRating = garageService.getAverageRating(id);

        model.addAttribute("garage", garage);
        model.addAttribute("avgRating", avgRating != null ? String.format("%.1f", avgRating) : "N/A");

        if (userDetails != null) {
            model.addAttribute("loggedUserUsername", userDetails.getUsername());
        }

        return "garage/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('GARAGE_OWNER')")
    public String newGarageForm(Model model) {
        model.addAttribute("garageDTO", new GarageDTO());
        model.addAttribute("serviceTypes", ServiceType.values());
        return "garage/create";
    }

    @PostMapping("/new")
    @PreAuthorize("hasRole('GARAGE_OWNER')")
    public String createGarage(@Valid @ModelAttribute("garageDTO") GarageDTO dto,
                               BindingResult result,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("serviceTypes", ServiceType.values());
            return "garage/create";
        }
        try {
            Garage garage = garageService.createGarage(dto, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "Garage registered! Awaiting admin verification.");
            return "redirect:/garages/" + garage.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/garages/new";
        }
    }

    @GetMapping("/{id}/contact")
    public String contactGarageForm(@PathVariable Long id, Model model) {
        Garage garage = garageService.findById(id);
        model.addAttribute("garage", garage);
        model.addAttribute("contactDTO", new ContactGarageDTO());
        return "garage/contact";
    }

    @PostMapping("/{id}/contact")
    public String contactGarage(@PathVariable Long id,
                                @Valid @ModelAttribute("contactDTO") ContactGarageDTO dto,
                                BindingResult result,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fill in both subject and message.");
            return "redirect:/garages/" + id + "/contact";
        }

        Garage garage = garageService.findById(id);

        if (garage.getEmail() == null || garage.getEmail().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "This garage has not provided a contact email.");
            return "redirect:/garages/" + id + "/contact";
        }

        User sender = userService.findByEmail(userDetails.getUsername());

        emailService.sendContactGarageEmail(
                garage.getEmail(),
                sender.getFullName(),
                sender.getEmail(),
                dto.getSubject(),
                dto.getMessage()
        );

        redirectAttributes.addFlashAttribute("success", "Your message has been sent to the garage.");
        return "redirect:/garages/" + id + "/contact";
    }

    @GetMapping("/{id}/contact-customer")
    @PreAuthorize("hasRole('GARAGE_OWNER')")
    public String contactCustomerForm(@PathVariable Long id, Model model,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        Garage garage = garageService.findById(id);
        User owner = userService.findByEmail(userDetails.getUsername());

        if (!garage.getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("You don't own this garage");
        }

        model.addAttribute("garage", garage);
        model.addAttribute("contactCustomerDTO", new ContactCustomerDTO());
        return "garage/contact-customer";
    }

    @PostMapping("/{id}/contact-customer")
    @PreAuthorize("hasRole('GARAGE_OWNER')")
    public String contactCustomer(@PathVariable Long id,
                                  @Valid @ModelAttribute("contactCustomerDTO") ContactCustomerDTO dto,
                                  BindingResult result,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fill in all fields correctly.");
            return "redirect:/garages/" + id + "/contact-customer";
        }

        Garage garage = garageService.findById(id);
        User owner = userService.findByEmail(userDetails.getUsername());

        if (!garage.getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("You don't own this garage");
        }

        emailService.sendGarageToCustomerEmail(
                dto.getCustomerEmail(),
                garage.getGarageName(),
                garage.getEmail(),
                dto.getSubject(),
                dto.getMessage()
        );

        redirectAttributes.addFlashAttribute("success", "Your message has been sent to the customer.");
        return "redirect:/garages/" + id;
    }

    @GetMapping("/nearby")
    @ResponseBody
    public List<Garage> getNearbyGarages(@RequestParam Double lat, @RequestParam Double lng) {
        return garageService.findNearbyGarages(lat, lng, 15.0);
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('GARAGE_OWNER', 'ADMIN')")
    public String editGarageForm(@PathVariable Long id, Model model) {
        Garage garage = garageService.findById(id);
        GarageDTO dto = new GarageDTO();
        dto.setGarageName(garage.getGarageName());
        dto.setAddress(garage.getAddress());
        dto.setPhone(garage.getPhone());
        dto.setEmail(garage.getEmail());
        dto.setLatitude(garage.getLatitude());
        dto.setLongitude(garage.getLongitude());
        dto.setOpeningTime(garage.getOpeningTime());
        dto.setClosingTime(garage.getClosingTime());
        dto.setServices(garage.getServices());

        model.addAttribute("garageDTO", dto);
        model.addAttribute("garageId", id);
        model.addAttribute("serviceTypes", ServiceType.values());
        return "garage/edit";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('GARAGE_OWNER', 'ADMIN')")
    public String updateGarage(@PathVariable Long id,
                               @Valid @ModelAttribute("garageDTO") GarageDTO dto,
                               BindingResult result,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("garageId", id);
            model.addAttribute("serviceTypes", ServiceType.values());
            return "garage/edit";
        }
        try {
            garageService.updateGarage(id, dto, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "Garage updated successfully!");
            return "redirect:/garages/" + id;
        } catch (Exception e) {
            model.addAttribute("garageId", id);
            model.addAttribute("serviceTypes", ServiceType.values());
            model.addAttribute("error", e.getMessage());
            return "garage/edit";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('GARAGE_OWNER', 'ADMIN')")
    public String deleteGarage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        garageService.softDelete(id);
        redirectAttributes.addFlashAttribute("success", "Garage deleted.");
        return "redirect:/garages";
    }

    @PostMapping("/{id}/toggle-availability")
    @PreAuthorize("hasRole('GARAGE_OWNER')")
    public String toggleAvailability(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        garageService.toggleAvailability(id);
        redirectAttributes.addFlashAttribute("success", "Availability updated.");
        return "redirect:/garages/" + id;
    }
}