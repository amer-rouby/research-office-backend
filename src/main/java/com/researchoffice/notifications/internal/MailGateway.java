package com.researchoffice.notifications.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends plain-text emails through the configured SMTP server (e.g. Brevo free tier).
 * With no SMTP host configured, messages are only logged, so the app works without email.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MailGateway {

    private final ObjectProvider<JavaMailSender> sender;
    private final NotificationsProperties props;

    void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return;
        }
        JavaMailSender mail = sender.getIfAvailable();
        if (!props.mailEnabled() || mail == null) {
            log.info("[mail disabled] to={} subject={}", to, subject);
            return;
        }
        var message = new SimpleMailMessage();
        message.setFrom(props.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mail.send(message);
        } catch (MailException e) {
            // A failed email must never break the request flow; it is logged for follow-up.
            log.warn("Email to {} failed: {}", to, e.getMessage());
        }
    }

    String adminEmail() {
        return props.adminEmail();
    }

    @ConfigurationProperties("ro.notifications")
    record NotificationsProperties(String mailHost, String from, String adminEmail) {
        boolean mailEnabled() {
            return mailHost != null && !mailHost.isBlank() && from != null && !from.isBlank();
        }
    }
}
