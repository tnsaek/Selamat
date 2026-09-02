package com.tinsae.socialmediaplatform.common.service;

import com.tinsae.socialmediaplatform.common.config.AppMailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(AppMailProperties.class)
public class SmtpEmailService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailService.class);

    private final AppMailProperties mailProperties;
    private final JavaMailSender mailSender;

    public SmtpEmailService(AppMailProperties mailProperties, JavaMailSender mailSender) {
        this.mailProperties = mailProperties;
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetLink) {
        if (!mailProperties.isEnabled()) {
            LOGGER.info("Password reset link for {}: {}", recipientEmail, resetLink);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(recipientEmail);
        message.setSubject("Reset your Selamat password");
        message.setText("""
                We received a request to reset your Selamat password.

                Use this link to choose a new password:
                %s

                This link expires in 30 minutes. If you did not request this, you can ignore this email.
                """.formatted(resetLink));

        mailSender.send(message);
    }
}
