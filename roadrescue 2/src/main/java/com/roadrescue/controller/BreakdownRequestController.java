package com.roadrescue.controller;

import com.roadrescue.dto.BreakdownRequestDTO;
import com.roadrescue.entity.*;
import com.roadrescue.enums.RequestStatus;
import com.roadrescue.enums.ServiceType;
import com.roadrescue.service.*;
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

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/requests")
@RequiredArgsConstructor
public class BreakdownRequestController {

    private final BreakdownRequestService requestService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final PaymentService paymentService;

    @GetMapping
    public String listRequests(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        Page<BreakdownRequest> requests = requestService.getRequestsByUser(user.getId(), page, size);
        model.addAttribute("requests", requests);
        model.addAttribute("currentPage", page);
        model.addAttribute("user", user);
        return "request/list";
    }

    @GetMapping("/new")
    public String newRequestForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("requestDTO", new BreakdownRequestDTO());
        model.addAttribute("serviceTypes", ServiceType.values());
        model.addAttribute("userVehicles", vehicleService.getUserVehicles(user.getId()));
        return "request/create";
    }

    @PostMapping("/new")
    public String createRequest(@Valid @ModelAttribute("requestDTO") BreakdownRequestDTO dto,
                                BindingResult result,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("serviceTypes", ServiceType.values());
            model.addAttribute("userVehicles", vehicleService.getUserVehicles(
                    userService.findByEmail(userDetails.getUsername()).getId()));
            return "request/create";
        }
        try {
            BreakdownRequest request = requestService.createRequest(dto, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "Request submitted successfully!");
            return "redirect:/requests/" + request.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/requests/new";
        }
    }

    @GetMapping("/{id}")
    public String viewRequest(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        BreakdownRequest request = requestService.findById(id);
        User user = userService.findByEmail(userDetails.getUsername());

        List<Garage> nearbyGarages = requestService.findNearbyGarages(
                request.getLatitude(), request.getLongitude(), request.getServiceType());

        Payment payment = paymentService.findByRequestId(id);

        model.addAttribute("request", request);
        model.addAttribute("nearbyGarages", nearbyGarages);
        model.addAttribute("user", user);
        model.addAttribute("payment", payment);
        return "request/detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancelRequest(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        requestService.cancelRequest(id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Request cancelled.");
        return "redirect:/requests";
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'GARAGE_OWNER')")
    public String allRequests(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String serviceType,
                              Model model) {
        RequestStatus statusEnum = status != null ? RequestStatus.valueOf(status) : null;
        ServiceType serviceTypeEnum = serviceType != null ? ServiceType.valueOf(serviceType) : null;

        Page<BreakdownRequest> requests = requestService.getAllRequests(page, size, statusEnum, serviceTypeEnum);
        model.addAttribute("requests", requests);
        model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("serviceTypes", ServiceType.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedServiceType", serviceType);
        model.addAttribute("currentPage", page);
        return "admin/requests";
    }
}