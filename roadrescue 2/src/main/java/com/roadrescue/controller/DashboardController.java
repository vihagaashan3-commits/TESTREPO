package com.roadrescue.controller;

import com.roadrescue.entity.User;
import com.roadrescue.enums.RequestStatus;
import com.roadrescue.enums.Role;
import com.roadrescue.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final BreakdownRequestService requestService;
    private final GarageService garageService;
    private final NotificationService notificationService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/About")
    public String about() {
        return "about";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getId()));

        String role = user.getRole().name();

        if (role.equals(Role.ROLE_ADMIN.name())) {
            model.addAttribute("totalUsers", userService.countByRole(Role.ROLE_USER));
            model.addAttribute("totalGarages", garageService.getTotalCount());
            model.addAttribute("totalRequests", requestService.getTotalCount());
            model.addAttribute("pendingRequests", requestService.getCountByStatus(RequestStatus.PENDING));
            return "admin/dashboard";
        }

        if (role.equals(Role.ROLE_GARAGE_OWNER.name())) {
            model.addAttribute("myGarages", garageService.getOwnerGarages(userDetails.getUsername()));
            return "garage-owner/dashboard";
        }



        model.addAttribute("myRequests", requestService.getRequestsByUser(user.getId(), 0, 5));
        return "user/dashboard";
    }
}
