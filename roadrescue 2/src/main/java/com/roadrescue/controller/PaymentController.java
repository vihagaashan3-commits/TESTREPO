package com.roadrescue.controller;

import com.roadrescue.entity.*;
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

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    @GetMapping
    public String listPayments(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        Page<Payment> payments = paymentService.getUserPayments(user.getId(), page, 10);
        model.addAttribute("payments", payments);
        model.addAttribute("currentPage", page);
        return "payment/list";
    }

    @GetMapping("/request/{requestId}/create")
    @PreAuthorize("hasAnyRole('GARAGE_OWNER', 'ADMIN')")
    public String createPaymentForm(@PathVariable Long requestId, Model model) {
        model.addAttribute("requestId", requestId);
        return "payment/create";
    }

    @PostMapping("/request/{requestId}/create")
    @PreAuthorize("hasAnyRole('GARAGE_OWNER', 'ADMIN')")
    public String createPayment(@PathVariable Long requestId,
                                @RequestParam BigDecimal amount,
                                @RequestParam String method,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes ra) {
        try {
            Payment payment = paymentService.createPayment(requestId, amount, method, userDetails.getUsername());
            ra.addFlashAttribute("success", "Payment record created. ID: #" + payment.getId());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/requests/" + requestId;
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('GARAGE_OWNER', 'ADMIN')")
    public String confirmPayment(@PathVariable Long id, RedirectAttributes ra) {
        Payment payment = paymentService.markAsPaid(id);
        ra.addFlashAttribute("success", "Payment confirmed!");
        return "redirect:/requests/" + payment.getBreakdownRequest().getId();
    }
}
