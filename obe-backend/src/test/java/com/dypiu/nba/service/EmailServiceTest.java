package com.dypiu.nba.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://pbas.dypiu.ac.in");
        ReflectionTestUtils.setField(emailService, "fromEmail", "DYPIU OBE System <noreply@dypiu.ac.in>");
    }

    @Test
    @DisplayName("HTML Generation: buildResetEmailHtml succeeds with literal % in CSS and HTML without UnknownFormatConversionException")
    void testBuildResetEmailHtmlSucceedsWithoutException() {
        String recipientName = "Dr. Jane Doe";
        String resetLink = "https://pbas.dypiu.ac.in/reset-password?token=abcdef1234567890securetoken";

        assertThatCode(() -> {
            String html = emailService.buildResetEmailHtml(recipientName, resetLink);
            assertThat(html).isNotNull().isNotEmpty();
            assertThat(html).contains("Dear <strong>Dr. Jane Doe</strong>");
            assertThat(html).contains(resetLink);
            assertThat(html).contains("width=\"100%\"");
            assertThat(html).contains("15 minutes");
            assertThat(html).contains("D. Y. Patil International University");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("HTML Generation: Handles null or blank recipient name with fallback")
    void testBuildResetEmailHtmlHandlesBlankRecipient() {
        String resetLink = "https://pbas.dypiu.ac.in/reset-password?token=tok123";

        String html = emailService.buildResetEmailHtml(null, resetLink);
        assertThat(html).contains("Dear <strong>Faculty / Member</strong>");
        assertThat(html).contains(resetLink);

        String html2 = emailService.buildResetEmailHtml("   ", resetLink);
        assertThat(html2).contains("Dear <strong>Faculty / Member</strong>");
    }

    @Test
    @DisplayName("Password Reset Dispatch: sendPasswordResetEmail builds MIME message and reaches JavaMailSender")
    void testSendPasswordResetEmailDispatchesCorrectly() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String recipientEmail = "faculty.member@dypiu.ac.in";
        String recipientName = "Prof. Alan Turing";
        String rawToken = "dypiu_test_token_256bit_secure_random";

        assertThatCode(() -> {
            emailService.sendPasswordResetEmail(recipientEmail, recipientName, rawToken);
        }).doesNotThrowAnyException();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Password Reset Dispatch: Skips dispatch safely when mailSender or recipient email is null/empty")
    void testSendPasswordResetEmailGracefulHandling() {
        EmailService serviceNoSender = new EmailService(null);
        assertThatCode(() -> {
            serviceNoSender.sendPasswordResetEmail("user@dypiu.ac.in", "User", "token123");
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            emailService.sendPasswordResetEmail(null, "User", "token123");
            emailService.sendPasswordResetEmail("   ", "User", "token123");
        }).doesNotThrowAnyException();

        verifyNoInteractions(mailSender);
    }
}
