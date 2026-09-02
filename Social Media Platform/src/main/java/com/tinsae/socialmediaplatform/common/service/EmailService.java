package com.tinsae.socialmediaplatform.common.service;

public interface EmailService {

    void sendPasswordResetEmail(String recipientEmail, String resetLink);
}
