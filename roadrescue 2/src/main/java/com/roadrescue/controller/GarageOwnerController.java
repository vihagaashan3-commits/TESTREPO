package com.roadrescue.controller;

import com.roadrescue.entity.*;
import com.roadrescue.enums.RequestStatus;
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

    @GetMapping("/requests")
    public String listRequests(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        List<Garage> myGarages = garageService.getOwnerGarages(userDetails.getUsername());

        if (!myGarages.isEmpty()) {
            Long garageId = myGarages.get(0).getId();

            // Requests assigned to this garage
            Page<BreakdownRequest> myRequests = requestService.getRequestsByGarage(garageId, page, size);

            // All pending requests (not yet assigned to any garage)
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

        ra.addFlashAttribute("success", "Request #" + id + " accepted!");
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
        ra.addFlashAttribute("error", "Request #" + id + " declined. User has been notified.");
        return "redirect:/garage-owner/requests";
    }

    @PostMapping("/requests/{id}/in-progress")
    public String markInProgress(@PathVariable Long id, RedirectAttributes ra) {
        requestService.updateStatus(id, RequestStatus.IN_PROGRESS);
        ra.addFlashAttribute("success", "Request marked as In Progress.");
        return "redirect:/garage-owner/requests";
    }

    @PostMapping("/requests/{id}/set-final-amount")
    public String setFinalAmount(@PathVariable Long id,
                                 @RequestParam BigDecimal finalAmount,
                                 RedirectAttributes ra) {
        requestService.setFinalAmount(id, finalAmount);
        ra.addFlashAttribute("success", "Final amount set. Customer has been notified.");
        return "redirect:/garage-owner/requests";
    }

    @PostMapping("/requests/{id}/complete")
    public String markComplete(@PathVariable Long id, RedirectAttributes ra) {
        BreakdownRequest req = requestService.updateStatus(id, RequestStatus.COMPLETED);
        emailService.sendRequestCompletedEmail(req.getUser().getEmail(), req.getUser().getFullName());
        ra.addFlashAttribute("success", "Request marked as Completed!");
        return "redirect:/garage-owner/requests";
    }

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
}