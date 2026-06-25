package com.roadrescue.controller;

import com.roadrescue.service.ReviewService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/garage/{garageId}")
    public String reviewForm(@PathVariable Long garageId, Model model) {
        model.addAttribute("garageId", garageId);
        return "review/create";
    }

    @PostMapping("/garage/{garageId}")
    public String submitReview(@PathVariable Long garageId,
                               @RequestParam @Min(1) @Max(5) Integer rating,
                               @RequestParam @NotBlank String comment,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes ra) {
        try {
            reviewService.addReview(garageId, userDetails.getUsername(), rating, comment);
            ra.addFlashAttribute("success", "Review submitted! Thank you.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/garages/" + garageId;
    }

    @PostMapping("/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                               @RequestParam Long garageId,
                               RedirectAttributes ra) {
        reviewService.softDelete(id);
        ra.addFlashAttribute("success", "Review deleted.");
        return "redirect:/garages/" + garageId;
    }
}
