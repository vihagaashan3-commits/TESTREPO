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

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("RoadRescue <nadeeshakalhara685@gmail.com>");
            helper.setTo(to);
            helper.setSubject("Your RoadRescue Request Has Been Accepted");

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body{
                            margin:0;
                            padding:0;
                            background:#f4f6f9;
                            font-family:Arial,Helvetica,sans-serif;
                        }

                        .container{
                            max-width:650px;
                            margin:30px auto;
                            background:#ffffff;
                            border-radius:12px;
                            overflow:hidden;
                            box-shadow:0 5px 20px rgba(0,0,0,.1);
                        }

                        .header{
                            background:linear-gradient(135deg,#dc3545,#b71c1c);
                            color:white;
                            text-align:center;
                            padding:35px;
                        }

                        .header h1{
                            margin:0;
                            font-size:28px;
                        }

                        .body{
                            padding:35px;
                            color:#444;
                            line-height:1.8;
                            font-size:15px;
                        }

                        .status{
                            background:#e8f5e9;
                            border-left:5px solid #28a745;
                            padding:18px;
                            border-radius:8px;
                            margin:25px 0;
                        }

                        .status h3{
                            color:#28a745;
                            margin-top:0;
                        }

                        .garage-box{
                            background:#fff3cd;
                            border-left:5px solid #ffc107;
                            padding:18px;
                            border-radius:8px;
                            margin-top:20px;
                        }

                        .button{
                            display:inline-block;
                            padding:14px 35px;
                            background:#dc3545;
                            color:white !important;
                            text-decoration:none;
                            border-radius:8px;
                            font-weight:bold;
                            margin-top:30px;
                        }

                        .footer{
                            background:#f8f9fa;
                            padding:20px;
                            text-align:center;
                            color:#777;
                            font-size:13px;
                        }
                    </style>
                </head>

                <body>

                <div class="container">

                    <div class="header">
                        <h1>RoadRescue</h1>
                        <p>Vehicle Breakdown Assistance</p>
                    </div>

                    <div class="body">

                        <h2>Hello %s,</h2>

                        <p>
                        Great news! Your roadside assistance request has been
                        <strong style="color:#28a745;">accepted</strong>.
                        </p>

                        <div class="status">
                            <h3>✔ Request Confirmed</h3>

                            <p>
                            Our service partner has accepted your request and
                            is preparing to assist you.
                            </p>
                        </div>

                        <div class="garage-box">
                            <strong>Assigned Garage</strong><br><br>

                             <strong>%s</strong>
                        </div>

                        <p style="margin-top:30px;">
                        A technician will contact you shortly and head towards
                        your location.
                        </p>

                        <p>
                        Please keep your mobile phone available and stay at a safe location.
                        </p>

                        <center>
                            <a class="button" href="#">
                                View Request
                            </a>
                        </center>

                        <p style="margin-top:35px;">
                        Thank you for choosing <strong>RoadRescue</strong>.
                        We're committed to getting you back on the road safely.
                        </p>

                    </div>

                    <div class="footer">
                        © 2026 RoadRescue<br>
                        Fast • Reliable • Professional Roadside Assistance
                    </div>

                </div>

                </body>
                </html>
                """.formatted(userName, garageName);

            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            log.error("Failed to send Accepted Email to {}", to, e);
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
