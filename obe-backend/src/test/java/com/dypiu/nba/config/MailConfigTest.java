package com.dypiu.nba.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailConfigTest {

    @Test
    @DisplayName("mailSenderSanitizer trims leading and trailing whitespace from JavaMailSenderImpl properties")
    void testMailSenderSanitizerTrimsWhitespace() {
        MailConfig mailConfig = new MailConfig();
        BeanPostProcessor postProcessor = mailConfig.mailSenderSanitizer();

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("  smtp.gmail.com                                            ");
        sender.setUsername("  raj.shaikh.7709@gmail.com  ");
        sender.setPassword("  skplafxtowerysxh  ");
        sender.setProtocol("  smtp  ");

        postProcessor.postProcessAfterInitialization(sender, "javaMailSender");

        assertEquals("smtp.gmail.com", sender.getHost());
        assertEquals("raj.shaikh.7709@gmail.com", sender.getUsername());
        assertEquals("skplafxtowerysxh", sender.getPassword());
        assertEquals("smtp", sender.getProtocol());
    }
}
