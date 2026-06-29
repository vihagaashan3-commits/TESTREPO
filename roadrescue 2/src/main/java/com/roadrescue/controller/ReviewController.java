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

    // ── Existing: garage path-variable flow ──────────────────────────────────

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

    // ── New: query-param flow used by the breakdown request button ────────────

    @GetMapping("/new")
    public String newReviewForm(@RequestParam Long garageId,
                                @RequestParam Long requestId,
                                Model model) {
        model.addAttribute("garageId", garageId);
        model.addAttribute("requestId", requestId);
        return "review/create";  // templates/reviews/new.html
    }

    @PostMapping("/new")
    public String submitNewReview(@RequestParam Long garageId,
                                  @RequestParam Long requestId,
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
        return "redirect:/requests/" + requestId;
    }

    // ── Existing: delete ──────────────────────────────────────────────────────

    @PostMapping("/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                               @RequestParam Long garageId,
                               RedirectAttributes ra) {
        reviewService.softDelete(id);
        ra.addFlashAttribute("success", "Review deleted.");
        return "redirect:/garages/" + garageId;
    }
}