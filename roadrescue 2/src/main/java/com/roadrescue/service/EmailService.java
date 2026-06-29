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

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("nadeeshakalhara685@gmail.com", "RoadRescue");
            helper.setTo(to);
            helper.setSubject(" Welcome to RoadRescue!");

            String html = """
        <!DOCTYPE html>
        <html>
        <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;">

        <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
          <tr>
            <td align="center">

              <table width="650" cellpadding="0" cellspacing="0"
                     style="background:#ffffff;border-radius:12px;
                     overflow:hidden;
                     box-shadow:0 4px 12px rgba(0,0,0,.12);">

                <!-- Header -->
                <tr>
                  <td style="background:#0d6efd;padding:30px;text-align:center;color:white;">

                    <h1 style="margin:0;">RoadRescue</h1>

                    <p style="margin-top:10px;font-size:16px;">
                        Roadside Assistance You Can Trust
                    </p>

                  </td>
                </tr>

                <!-- Content -->
                <tr>
                  <td style="padding:40px;">

                    <h2 style="margin-top:0;">
                        Welcome, %s!
                    </h2>

                    <p>
                        Thank you for joining
                        <strong>RoadRescue</strong>.
                    </p>

                    <p>
                        We're delighted to have you as part of our community.
                        Whenever your vehicle needs assistance, RoadRescue is
                        ready to connect you with trusted roadside service providers.
                    </p>

                    <table width="100%%"
                           style="background:#f8f9fa;
                           border-left:4px solid #0d6efd;
                           padding:18px;
                           margin:25px 0;">

                      <tr>
                        <td>

                        <strong>With RoadRescue you can:</strong>

                        <ul style="margin-top:12px;line-height:1.8;">
                            <li>🚗 Request emergency roadside assistance</li>
                            <li>🔧 Connect with nearby trusted garages</li>
                            <li>📍 Track your service requests</li>
                            <li>⭐ Rate and review completed services</li>
                        </ul>

                        </td>
                      </tr>

                    </table>

                    <p>
                        We're committed to making every journey safer and more
                        convenient for you.
                    </p>

                    <p>
                        If you ever need assistance,
                        we're only a few clicks away.
                    </p>

                    <br>

                    <p>
                    Best Regards,<br>
                    <strong>RoadRescue Support Team</strong><br>
                    <span style="color:#666;">
                    Keeping You Moving, Anytime, Anywhere.
                    </span>
                    </p>

                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="background:#f8f9fa;
                  text-align:center;
                  padding:20px;
                  color:#888;
                  font-size:13px;">

                  © 2026 RoadRescue. All Rights Reserved.

                  </td>
                </tr>

              </table>

            </td>
          </tr>
        </table>

        </body>
        </html>
        """.formatted(name);

            helper.setText(html, true);

            mailSender.send(mimeMessage);

            log.info("Welcome email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", to, e);
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
            <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;">
            
            <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
              <tr>
                <td align="center">
            
                  <table width="600" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:12px;
                         overflow:hidden;box-shadow:0 8px 25px rgba(0,0,0,.08);">
            
                    <!-- HEADER -->
                    <tr>
                      <td style="background:#dc3545;padding:25px;text-align:center;">
                                <h1 style="color:white;margin:0;">

                        <h1 style="margin:0;"> RoadRescue</h1>
                     </td>
                    </tr>
            
                    <!-- BODY -->
                    <tr>
                      <td style="padding:35px;">
            
                        <h2>Hello %s,</h2>
            
                        <p>
                          Great news! Your roadside assistance request has been
                          <b style="color:#28a745;">accepted</b>.
                        </p>
            
                        <div style="
                            background:#e8f5e9;
                            border-left:5px solid #28a745;
                            padding:15px;
                            border-radius:8px;
                            margin:20px 0;
                        ">
                          <b>✔ Request Confirmed</b><br>
                          Our service partner has accepted your request and is preparing to assist you.
                        </div>
            
                        <div style="
                            background:#fff3cd;
                            border-left:5px solid #ffc107;
                            padding:15px;
                            border-radius:8px;
                            margin:20px 0;
                        ">
                          <b>Assigned Garage</b><br><br>
                          <b>%s</b>
                        </div>
            
                        <p>
                          A technician will contact you shortly and head towards your location.
                        </p>
            
                        <p>
                          Please keep your mobile phone available and stay at a safe location.
                        </p>
            
                        <p style="text-align:center;margin:35px 0;">
                          <a href="#"
                             style="
                             background:#dc3545;
                             color:white;
                             text-decoration:none;
                             padding:14px 30px;
                             border-radius:8px;
                             font-weight:bold;">
                             View Request
                          </a>
                        </p>
            
                        <p>
                          Thank you for choosing <b>RoadRescue</b>.
                        </p>
            
                        <p style="color:#777;">
                          Fast • Reliable • Professional Roadside Assistance
                        </p>
            
                      </td>
                    </tr>
            
                    <!-- FOOTER -->
                    <tr>
                      <td style="
                          background:#f8f9fa;
                          padding:18px;
                          text-align:center;
                          color:#888;
                          font-size:13px;">
                        © 2026 RoadRescue. All Rights Reserved.
                      </td>
                    </tr>
            
                  </table>
            
                </td>
              </tr>
            </table>
            
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
    public void sendRequestCompletedEmail(
            String to,
            String userName,
            Long garageId,
            Long requestId) {

        try {
            String reviewLink = "http://localhost:8080/reviews/new?garageId="
                    + garageId + "&requestId=" + requestId;

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("nadeeshakalhara685@gmail.com", "RoadRescue");
            helper.setTo(to);
            helper.setSubject("Your RoadRescue Service Has Been Completed");

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

                        <h1 style="margin:0;"> RoadRescue</h1>
                     </td>
                    </tr>

                    <tr>
                      <td style="padding:35px;">

                        <h2>Hello %s,</h2>

                        <p>
                        We are pleased to inform you that your roadside
                        assistance request has been <b>successfully completed.</b>
                        </p>

                        <p>
                        Thank you for choosing <b>RoadRescue</b>.
                        We hope our service helped you get back on the road
                        safely and quickly.
                        </p>

                        <p>
                        Your opinion is important to us.
                        Please take a moment to rate your experience.
                        </p>

                        <p style="text-align:center;margin:40px 0;">
                          <a href="%s"
                             style="
                             background:#0d6efd;
                             color:white;
                             text-decoration:none;
                             padding:16px 34px;
                             border-radius:8px;
                             font-size:16px;
                             font-weight:bold;">
                             ⭐ Leave a Review
                          </a>
                        </p>

                        <hr>

                        <p style="color:#666;">
                        Your feedback helps us improve our services and
                        continue providing reliable roadside assistance.
                        </p>

                        <p>
                        Thank you once again for choosing RoadRescue.
                        </p>

                        <br>

                        <p>
                        Best Regards,<br>
                        <b>RoadRescue Support Team</b><br>
                        <span style="color:#777;">
                        Keeping You Moving, Anytime, Anywhere.
                        </span>
                        </p>

                      </td>
                    </tr>

                    <tr>
                      <td style="
                      background:#f8f9fa;
                      padding:18px;
                      text-align:center;
                      color:#888;
                      font-size:13px;">

                      © 2026 RoadRescue. All Rights Reserved.

                      </td>
                    </tr>

                  </table>

                </td>
              </tr>
            </table>

            </body>
            </html>
            """.formatted(userName, reviewLink);

            helper.setText(html, true);

            mailSender.send(mimeMessage);

            log.info("Request completion email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send request completion email to {}", to, e);
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
