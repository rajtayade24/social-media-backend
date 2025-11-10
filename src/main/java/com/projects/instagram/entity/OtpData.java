package com.projects.instagram.entity;

import java.time.Instant;

/**
 * Simple model holding OTP code, expiry and attempts.
 */
public class OtpData {
    private final String code;
    private final Instant expiresAt;
    private int attempts;

    public OtpData(String code, Instant expiresAt, int attempts) {
        this.code = code;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
    }

    public String getCode() { return code; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getAttempts() { return attempts; }
    public void incrementAttempts() { this.attempts++; }
}
