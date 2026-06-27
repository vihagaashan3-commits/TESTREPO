package com.roadrescue.controller;

import com.roadrescue.entity.User;
import com.roadrescue.service.NotificationService;
import com.roadrescue.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String listNotifications(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam(defaultValue = "0") int page,
                                    Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("notifications",
                notificationService.getUserNotifications(user.getId(), page, 15));
        model.addAttribute("currentPage", page);
        model.addAttribute("user", user);
        return "notification/list";
    }
    @GetMapping("/unread")
    public String unreadNotifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("notifications", notificationService.getUnreadNotifications(user.getId()));
        model.addAttribute("user", user);
        return "notification/unread";
    }
    @PostMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public String markAllRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        notificationService.markAllAsRead(user.getId());
        return "redirect:/notifications";
    }

    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
