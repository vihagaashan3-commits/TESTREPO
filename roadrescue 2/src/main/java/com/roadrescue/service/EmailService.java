package com.roadrescue.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
            message.setFrom("nadeeshakalhara685@gmail.com");
            message.setTo(to);
            message.setSubject("Welcome to RoadRescue!");
            message.setText("Dear " + name + ",\n\nWelcome to RoadRescue! " +
                    "We're here to help you whenever your vehicle breaks down.\n\n" +
                    "Stay safe on the road!\n\nThe RoadRescue Team");
            mailSender.send(message);
            log.info("Welcome email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", to, e);
        }
    }

    @Async
    public void sendRequestAcceptedEmail(String to, String userName, String garageName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("nadeeshakalhara685@gmail.com");
            message.setTo(to);
            message.setSubject("Your Breakdown Request Has Been Accepted");
            message.setText("Dear " + userName + ",\n\n" +
                    "Great news! " + garageName + " has accepted your breakdown request.\n" +
                    "A technician will be dispatched to your location shortly.\n\n" +
                    "Please keep your phone available.\n\nThe RoadRescue Team");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", to, e);
        }
    }

    @Async
    public void sendRequestCompletedEmail(String to, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("nadeeshakalhara685@gmail.com");
            message.setTo(to);
            message.setSubject("Your Breakdown Request Has Been Completed");
            message.setText("Dear " + userName + ",\n\n" +
                    "Your breakdown request has been completed. " +
                    "We hope you're back on the road!\n\n" +
                    "Please take a moment to rate your experience.\n\nThe RoadRescue Team");
            mailSender.send(message);
        }catch (Exception e) {
            log.error("Failed to send OTP email to {}", to, e);
        }
    }
    @Async
    public void sendOtpEmail(String to, String otp) {

        try {

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("nadeeshakalhara685@gmail.com");
            helper.setTo(to);
            helper.setSubject("RoadRescue Verification Code");

            String html = """
                    <!DOCTYPE html>
                    <html>
                    
                    <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;">

                    <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
                      <tr>
                        <td align="center">

                          <table width="600" cellpadding="0" cellspacing="0"
                                 style="background:#ffffff;border-radius:12px;
                                 overflow:hidden;box-shadow:0 8px 25px rgba(0,0,0,.08);">

                            <tr>
                              <td style="background:#dc3545;padding:25px;text-align:center;">
                                <h1 style="color:white;margin:0;">
                                    RoadRescue
                                </h1>
                              </td>
                            </tr>

                            <tr>
                              <td style="padding:40px;">

                                <h2 style="margin-top:0;color:#333;">
                                    Verification Required
                                </h2>

                                <p style="font-size:16px;color:#555;line-height:1.8;">
                                    We received a request to verify your account.
                                    Please use the verification code below.
                                </p>

                                <div style="
                                    background:#f8f9fa;
                                    border:2px dashed #dc3545;
                                    border-radius:10px;
                                    padding:20px;
                                    text-align:center;
                                    margin:30px 0;">

                                    <span style="
                                        font-size:38px;
                                        font-weight:bold;
                                        color:#dc3545;
                                        letter-spacing:8px;">
                                        %s
                                    </span>

                                </div>

                                <p style="color:#777;font-size:15px;">
                                     This code will expire in
                                    <strong>5 minutes</strong>.
                                </p>

                                <p style="color:#777;font-size:15px;">
                                    If you didn't request this verification,
                                    you can safely ignore this email.
                                </p>

                              </td>
                            </tr>

                            <tr>
                              <td style="
                                    background:#f8f9fa;
                                    text-align:center;
                                    padding:20px;
                                    color:#888;
                                    font-size:13px;">

                                  © 2026 RoadRescue<br>
                                  Fast • Reliable • Secure

                              </td>
                            </tr>

                          </table>

                        </td>
                      </tr>
                    </table>

                    </body>
                    </html>
                    """.formatted(otp);

            helper.setText(html, true);

            mailSender.send(message);

            log.info("OTP email sent to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}", to, e);
        }
    }
}
