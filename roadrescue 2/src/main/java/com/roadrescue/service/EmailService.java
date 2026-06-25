package com.roadrescue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendWelcomeEmail(String to, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to RoadRescue!");
            message.setText("Dear " + name + ",\n\nWelcome to RoadRescue! " +
                    "We're here to help you whenever your vehicle breaks down.\n\n" +
                    "Stay safe on the road!\n\nThe RoadRescue Team");
            mailSender.send(message);
            log.info("Welcome email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendRequestAcceptedEmail(String to, String userName, String garageName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Your Breakdown Request Has Been Accepted");
            message.setText("Dear " + userName + ",\n\n" +
                    "Great news! " + garageName + " has accepted your breakdown request.\n" +
                    "A technician will be dispatched to your location shortly.\n\n" +
                    "Please keep your phone available.\n\nThe RoadRescue Team");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send request accepted email: {}", e.getMessage());
        }
    }

    @Async
    public void sendRequestCompletedEmail(String to, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Your Breakdown Request Has Been Completed");
            message.setText("Dear " + userName + ",\n\n" +
                    "Your breakdown request has been completed. " +
                    "We hope you're back on the road!\n\n" +
                    "Please take a moment to rate your experience.\n\nThe RoadRescue Team");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send completion email: {}", e.getMessage());
        }
    }
}
