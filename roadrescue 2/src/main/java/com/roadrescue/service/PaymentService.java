package com.roadrescue.service;

import com.roadrescue.entity.*;
import com.roadrescue.enums.PaymentStatus;
import com.roadrescue.enums.RequestStatus;
import com.roadrescue.exception.ResourceNotFoundException;
import com.roadrescue.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final BreakdownRequestService requestService;
    private final NotificationService notificationService;

    @Transactional
    public Payment createPayment(Long requestId, BigDecimal amount,
                                 String method, String userEmail) {
        BreakdownRequest request = requestService.findById(requestId);
        User user = userService.findByEmail(userEmail);

        Payment payment = Payment.builder()
                .breakdownRequest(request)
                .user(user)
                .amount(amount)
                .paymentMethod(method)
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);

        notificationService.createNotification(user,
                "Payment Due",
                "Job completed. Please pay Rs. " + amount + " via " + method + ".",
                "PAYMENT_DUE", request);

        return saved;
    }

    @Transactional
    public Payment markAsPaid(Long paymentId) {
        Payment payment = findById(paymentId);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        BreakdownRequest request = payment.getBreakdownRequest();
        request.setStatus(RequestStatus.PAID);

        notificationService.createNotification(payment.getUser(),
                "Payment Confirmed",
                "Payment of Rs. " + payment.getAmount() + " confirmed. Thank you!",
                "PAYMENT_SUCCESS", request);

        return saved;
    }


    @Transactional
    public Payment processPayment(Long requestId, String paymentMethod, User user) {

        BreakdownRequest request = requestService.findById(requestId);

        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new RuntimeException("Payment not allowed at this stage.");
        }

        if (request.getQuoteAmount() == null) {
            throw new RuntimeException("No quote amount found.");
        }

        Payment existingPayment = paymentRepository.findByBreakdownRequestId(requestId).orElse(null);
        if (existingPayment != null) {
            throw new RuntimeException("Payment already made for this request.");
        }

        Payment payment = Payment.builder()
                .breakdownRequest(request)
                .user(user)
                .amount(request.getQuoteAmount())
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        request.setStatus(RequestStatus.PAID);
        request.setFinalAmount(request.getQuoteAmount());

        notificationService.createNotification(
                request.getGarage().getOwner(),
                "Payment Received",
                "Payment of Rs. " + request.getQuoteAmount() + " received via " + paymentMethod + ". You can now complete the job.",
                "PAYMENT_SUCCESS",
                request
        );

        return payment;
    }

    public Payment findById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    public Payment findByRequestId(Long requestId) {
        return paymentRepository.findByBreakdownRequestId(requestId).orElse(null);
    }

    public Page<Payment> getUserPayments(Long userId, int page, int size) {
        return paymentRepository.findByUserId(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public BigDecimal getTotalRevenue() {
        BigDecimal total = paymentRepository.getTotalRevenue();
        return total != null ? total : BigDecimal.ZERO;
    }
}