package com.tinsae.socialmediaplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public class AppMailProperties {

    private boolean enabled;
    private String from = "no-reply@selamat.local";
    private String frontendResetPasswordUrl = "http://localhost:4200/reset-password";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrontendResetPasswordUrl() {
        return frontendResetPasswordUrl;
    }

    public void setFrontendResetPasswordUrl(String frontendResetPasswordUrl) {
        this.frontendResetPasswordUrl = frontendResetPasswordUrl;
    }
}
