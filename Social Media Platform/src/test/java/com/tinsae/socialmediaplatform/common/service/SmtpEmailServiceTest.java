package com.tinsae.socialmediaplatform.common.service;

import com.tinsae.socialmediaplatform.common.config.AppMailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SmtpEmailServiceTest {

    @Test
    void logsInsteadOfSendingWhenMailIsDisabled() {
        AppMailProperties properties = new AppMailProperties();
        properties.setEnabled(false);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpEmailService service = new SmtpEmailService(properties, mailSender);

        service.sendPasswordResetEmail("user@example.com", "http://localhost:4200/reset-password?token=abc");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendsPasswordResetEmailWhenMailIsEnabled() {
        AppMailProperties properties = new AppMailProperties();
        properties.setEnabled(true);
        properties.setFrom("no-reply@example.com");
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpEmailService service = new SmtpEmailService(properties, mailSender);

        service.sendPasswordResetEmail("user@example.com", "https://app.example.com/reset-password?token=abc");

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@example.com");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).isEqualTo("Reset your Selamat password");
        assertThat(message.getText()).contains("https://app.example.com/reset-password?token=abc");
    }
}
