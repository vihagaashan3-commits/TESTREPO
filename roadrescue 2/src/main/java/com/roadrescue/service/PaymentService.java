package com.roadrescue.service;

import com.roadrescue.entity.*;
import com.roadrescue.enums.PaymentStatus;
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
                "Payment Created",
                "Payment of Rs. " + amount + " created for Request #" + requestId,
                "PAYMENT_CREATED", request);

        return saved;
    }

    @Transactional
    public Payment markAsPaid(Long paymentId) {
        Payment payment = findById(paymentId);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        notificationService.createNotification(payment.getUser(),
                "Payment Successful",
                "Payment of Rs. " + payment.getAmount() + " confirmed.",
                "PAYMENT_SUCCESS", payment.getBreakdownRequest());

        return saved;
    }

    public Payment findById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
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
