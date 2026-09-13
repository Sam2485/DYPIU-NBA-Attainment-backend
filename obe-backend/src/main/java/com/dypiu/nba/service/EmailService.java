package com.dypiu.nba.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail.from:DYPIU OBE System <raj.shaikh.7709@gmail.com>}")
    private String fromEmail;

    /**
     * Dispatches a password reset email asynchronously with a branded HTML template.
     *
     * @param recipientEmail User's verified email address
     * @param recipientName  User's full name
     * @param rawToken       URL-safe 256-bit single-use reset token
     */
    @Async
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String rawToken) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[EmailService] Cannot send password reset email: recipient email is empty");
            return;
        }

        if (mailSender == null) {
            log.warn("[EmailService] JavaMailSender bean is not configured; skipping email dispatch to {}", recipientEmail);
            return;
        }

        try {
            String baseUrl = frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl.replaceAll("/+$", "") : "http://localhost:5173";
            String resetLink = baseUrl + "/reset-password?token=" + rawToken;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (fromEmail != null && fromEmail.contains("<") && fromEmail.contains(">")) {
                int start = fromEmail.indexOf("<");
                int end = fromEmail.indexOf(">");
                String address = fromEmail.substring(start + 1, end).trim();
                String personal = fromEmail.substring(0, start).trim();
                helper.setFrom(address, personal);
            } else if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail.trim());
            } else {
                helper.setFrom("raj.shaikh.7709@gmail.com", "DYPIU OBE System");
            }
            helper.setTo(recipientEmail.trim());
            helper.setSubject("DYPIU OBE System — Password Reset Request");
            helper.setText(buildResetEmailHtml(recipientName, resetLink), true);

            mailSender.send(message);
            log.info("[EmailService] Password reset email successfully dispatched to: {}", recipientEmail);
        } catch (Exception e) {
            log.error("[EmailService] Failed to dispatch password reset email to {}: {}", recipientEmail, e.getMessage(), e);
        }
    }

    private String buildResetEmailHtml(String recipientName, String resetLink) {
        String name = recipientName != null && !recipientName.isBlank() ? recipientName.trim() : "Faculty / Member";
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Password Reset</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#f1f5f9;padding:40px 15px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" style="max-width:580px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px rgba(0,0,0,0.06);border:1px solid #e2e8f0;" cellspacing="0" cellpadding="0">
                      <!-- Header -->
                      <tr>
                        <td style="background:linear-gradient(135deg, #1e1b4b 0%%, #312e81 50%%, #4338ca 100%%);padding:36px 30px;text-align:center;color:#ffffff;">
                          <h1 style="margin:0;font-size:22px;font-weight:800;letter-spacing:-0.02em;line-height:1.2;">D. Y. Patil International University</h1>
                          <p style="margin:6px 0 0;font-size:13px;color:#c7d2fe;font-weight:600;letter-spacing:0.04em;text-transform:uppercase;">Outcome-Based Education (OBE) Attainment System</p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:36px 32px;">
                          <h2 style="margin:0 0 16px;font-size:18px;font-weight:700;color:#0f172a;">Password Reset Request</h2>
                          <p style="margin:0 0 16px;font-size:14.5px;line-height:1.6;color:#334155;">
                            Dear <strong>%s</strong>,
                          </p>
                          <p style="margin:0 0 24px;font-size:14.5px;line-height:1.6;color:#475569;">
                            We received a request to reset the password for your DYPIU OBE Portal account. Click the button below to set a new password:
                          </p>

                          <!-- Action Button -->
                          <table role="presentation" cellspacing="0" cellpadding="0" style="margin:28px 0;width:100%%;">
                            <tr>
                              <td align="center">
                                <a href="%s" style="display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;font-size:15px;font-weight:700;padding:14px 32px;border-radius:10px;box-shadow:0 4px 12px rgba(79,70,229,0.3);letter-spacing:0.01em;">
                                  Reset My Password
                                </a>
                              </td>
                            </tr>
                          </table>

                          <!-- Expiration & Security Notice -->
                          <div style="background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid #4f46e5;border-radius:8px;padding:14px 16px;margin:24px 0;">
                            <p style="margin:0;font-size:12.5px;line-height:1.5;color:#475569;">
                              <strong>⏱ Time Sensitive:</strong> This password reset link is valid for <strong>15 minutes</strong> and can only be used once.
                            </p>
                          </div>

                          <p style="margin:20px 0 8px;font-size:12.5px;color:#64748b;">
                            If the button above does not work, copy and paste this link into your browser:
                          </p>
                          <p style="margin:0 0 24px;font-size:12px;color:#4f46e5;word-break:break-all;">
                            <a href="%s" style="color:#4f46e5;text-decoration:underline;">%s</a>
                          </p>

                          <p style="margin:0;font-size:13px;line-height:1.5;color:#94a3b8;border-top:1px solid #f1f5f9;padding-top:20px;">
                            If you did not request a password reset, please ignore this email or contact the IQAC / System Administrator immediately.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f8fafc;padding:20px 30px;text-align:center;border-top:1px solid #e2e8f0;font-size:12px;color:#64748b;">
                          <p style="margin:0;">D. Y. Patil International University, Akurdi, Pune - 411044</p>
                          <p style="margin:4px 0 0;font-size:11px;color:#94a3b8;">Automated System Notification • Please do not reply directly to this email</p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(name, resetLink, resetLink, resetLink);
    }
}
