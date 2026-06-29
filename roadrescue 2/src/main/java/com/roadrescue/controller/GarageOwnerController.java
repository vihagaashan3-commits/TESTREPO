package com.roadrescue.controller;

import com.roadrescue.entity.*;
import com.roadrescue.enums.RequestStatus;
import com.roadrescue.enums.ServiceType;
import com.roadrescue.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/garage-owner")
@PreAuthorize("hasRole('GARAGE_OWNER')")
@RequiredArgsConstructor
public class GarageOwnerController {

    private final GarageService garageService;
    private final BreakdownRequestService requestService;
    private final TechnicianService technicianService;
    private final UserService userService;
    private final EmailService emailService;

    // ─────────────────────────────────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String ownerDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Garage> myGarages = garageService.getOwnerGarages(userDetails.getUsername());
        model.addAttribute("myGarages", myGarages);

        if (!myGarages.isEmpty()) {
            Long garageId = myGarages.get(0).getId();
            Page<BreakdownRequest> pending = requestService.getRequestsByGarage(garageId, 0, 10);
            model.addAttribute("pendingRequests", pending);
        }
        return "garage-owner/dashboard";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REQUESTS
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/requests")
    public String listRequests(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        List<Garage> myGarages = garageService.getOwnerGarages(userDetails.getUsername());

        if (!myGarages.isEmpty()) {
            Long garageId = myGarages.get(0).getId();

            Page<BreakdownRequest> myRequests = requestService.getRequestsByGarage(garageId, page, size);
            Page<BreakdownRequest> allPending = requestService.getAllRequests(page, 50, RequestStatus.PENDING, null);

            model.addAttribute("requests", myRequests);
            model.addAttribute("pendingRequests", allPending);
            model.addAttribute("garage", myGarages.get(0));
            model.addAttribute("myGarages", myGarages);
            model.addAttribute("currentPage", page);
        }
        return "garage-owner/requests";
    }

    @PostMapping("/requests/{id}/accept")
    public String acceptRequest(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes ra) {
        List<Garage> garages = garageService.getOwnerGarages(userDetails.getUsername());
        if (garages.isEmpty()) {
            ra.addFlashAttribute("error", "You don't have a registered garage.");
            return "redirect:/dashboard";
        }
        Garage garage = garages.get(0);
        BreakdownRequest req = requestService.acceptRequest(id, garage.getId());

        emailService.sendRequestAcceptedEmail(
                req.getUser().getEmail(),
                req.getUser().getFullName(),
                garage.getGarageName()
        );

        ra.addFlashAttribute("success", "Request #" + id + " accepted! Now send the driver a quote.");
        return "redirect:/garage-owner/requests";
    }

    @PostMapping("/requests/{id}/decline")
    public String declineRequest(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes ra) {
        List<Garage> garages = garageService.getOwnerGarages(userDetails.getUsername());
        if (garages.isEmpty()) {
            ra.addFlashAttribute("error", "You don't have a registered garage.");
            return "redirect:/dashboard";
        }
        requestService.declineRequest(id, garages.get(0).getId());
        ra.addFlashAttribute("info", "Request #" + id + " declined. Driver has been notified.");
        return "redirect:/garage-owner/requests";
    }

    @PostMapping("/requests/{id}/send-quote")
    public String sendQuote(@PathVariable Long id,
                            @RequestParam BigDecimal quoteAmount,
                            @RequestParam(required = false) String quoteNotes,
                            RedirectAttributes ra) {
        requestService.sendQuote(id, quoteAmount, quoteNotes);
        ra.addFlashAttribute("success", "Quote of Rs. " + quoteAmount + " sent to driver. Waiting for approval.");
        return "redirect:/garage-owner/requests";
    }

    @PostMapping("/requests/{id}/start-work")
    public String startWork(@PathVariable Long id, RedirectAttributes ra) {
        requestService.startWork(id);
        ra.addFlashAttribute("success", "Technician dispatched! Request is now In Progress.");
        return "redirect:/garage-owner/requests";
    }

    @PostMapping("/requests/{id}/complete")
    public String markComplete(@PathVariable Long id, RedirectAttributes ra) {

        BreakdownRequest req = requestService.completeJob(id);

        emailService.sendRequestCompletedEmail(
                req.getUser().getEmail(),
                req.getUser().getFullName(),
                req.getGarage().getId(),
                req.getId()
        );

        ra.addFlashAttribute(
                "success",
                "Request #" + id + " marked as Completed! Driver can now rate your service."
        );

        return "redirect:/garage-owner/requests";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TECHNICIANS — CRUD
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/technicians")
    public String manageTechnicians(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam(defaultValue = "0") int page,
                                    Model model) {
        List<Garage> garages = garageService.getOwnerGarages(userDetails.getUsername());
        if (!garages.isEmpty()) {
            Long garageId = garages.get(0).getId();
            model.addAttribute("technicians", technicianService.getByGarage(garageId, page, 10));
            model.addAttribute("garageId", garageId);
            model.addAttribute("currentPage", page);
        }
        model.addAttribute("newTechnician", new Technician());
        return "garage-owner/technicians";
    }

    @PostMapping("/technicians/add")
    public String addTechnician(@ModelAttribute Technician technician,
                                @RequestParam(value = "skills", required = false) List<String> skillNames,
                                @RequestParam Long garageId,
                                RedirectAttributes ra) {
        try {
            if (skillNames != null) {
                List<ServiceType> skills = skillNames.stream()
                        .map(ServiceType::valueOf)
                        .collect(Collectors.toList());
                technician.setSkills(skills);
            }
            technicianService.addTechnician(technician, garageId);
            ra.addFlashAttribute("success", "Technician \"" + technician.getName() + "\" added successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to add technician: " + e.getMessage());
        }
        return "redirect:/garage-owner/technicians";
    }

    @PostMapping("/technicians/{id}/update")
    public String updateTechnician(@PathVariable Long id,
                                   @RequestParam String name,
                                   @RequestParam String phone,
                                   @RequestParam(required = false) Integer experienceYears,
                                   @RequestParam(value = "skills", required = false) List<String> skillNames,
                                   RedirectAttributes ra) {
        try {
            Technician updated = new Technician();
            updated.setName(name);
            updated.setPhone(phone);
            updated.setExperienceYears(experienceYears);
            if (skillNames != null) {
                List<ServiceType> skills = skillNames.stream()
                        .map(ServiceType::valueOf)
                        .collect(Collectors.toList());
                updated.setSkills(skills);
            } else {
                updated.setSkills(List.of());
            }
            technicianService.update(id, updated);
            ra.addFlashAttribute("success", "Technician updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update technician: " + e.getMessage());
        }
        return "redirect:/garage-owner/technicians";
    }

    @PostMapping("/technicians/{id}/toggle")
    public String toggleAvailability(@PathVariable Long id, RedirectAttributes ra) {
        try {
            technicianService.toggleAvailability(id);
            ra.addFlashAttribute("success", "Technician availability updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not update availability: " + e.getMessage());
        }
        return "redirect:/garage-owner/technicians";
    }

    @PostMapping("/technicians/{id}/delete")
    public String deleteTechnician(@PathVariable Long id, RedirectAttributes ra) {
        try {
            technicianService.softDelete(id);
            ra.addFlashAttribute("success", "Technician removed from your garage.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not remove technician: " + e.getMessage());
        }
        return "redirect:/garage-owner/technicians";
    }
}
