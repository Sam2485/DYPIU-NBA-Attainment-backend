package com.dypiu.nba.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Defensive configuration for JavaMailSender to ensure environment variables
 * with inadvertent leading or trailing whitespace (common in .env copy-paste)
 * do not cause DNS resolution failures (e.g. UnknownHostException on "smtp.gmail.com   ").
 */
@Slf4j
@Configuration
public class MailConfig {

    @Bean
    public BeanPostProcessor mailSenderSanitizer() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof JavaMailSenderImpl mailSender) {
                    if (mailSender.getHost() != null) {
                        String cleanHost = mailSender.getHost().trim();
                        if (!cleanHost.equals(mailSender.getHost())) {
                            log.info("[MailConfig] Sanitized JavaMailSender host whitespace: from '{}' to '{}'",
                                    mailSender.getHost(), cleanHost);
                            mailSender.setHost(cleanHost);
                        }
                    }
                    if (mailSender.getUsername() != null) {
                        mailSender.setUsername(mailSender.getUsername().trim());
                    }
                    if (mailSender.getPassword() != null) {
                        mailSender.setPassword(mailSender.getPassword().trim());
                    }
                    if (mailSender.getProtocol() != null) {
                        mailSender.setProtocol(mailSender.getProtocol().trim());
                    }
                }
                return bean;
            }
        };
    }
}
