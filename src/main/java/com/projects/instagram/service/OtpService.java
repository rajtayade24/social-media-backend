package com.projects.instagram.service;

import com.projects.instagram.entity.OtpData;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Single service handling OTP generation, storage (in-memory), sending and
 * verification.
 * - Supports email or E.164 phone numbers
 * - TTL and max attempts injected from application.properties
 * - In production, replace in-memory store with Redis/DB
 */
@Service
@Data
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final EmailService emailService;
    private final SmsService smsService;

    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private final Map<String, Instant> verifiedStore = new ConcurrentHashMap<>(); // identifier -> expiry
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @Value("${otp.ttl.seconds:300}")
    private long ttlSeconds;

    @Value("${otp.max.attempts:5}")
    private int maxAttempts;

    private static final Pattern EMAIL_RX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern E164_RX = Pattern.compile("^\\+[1-9]\\d{6,14}$");
    private static final Pattern LOCAL_10_DIGIT = Pattern.compile("^\\d{10}$");

    private String generateOtp() {
        int num = secureRandom.nextInt(1_000_000);
        return String.format("%06d", num);
    }

    private enum Channel {
        EMAIL, SMS
    }

    private Channel detectChannel(String identifier) {
        if (identifier == null)
            throw new IllegalArgumentException("Identifier is required");
        identifier = identifier.trim();
        if (EMAIL_RX.matcher(identifier).matches())
            return Channel.EMAIL;
        if (E164_RX.matcher(identifier).matches())
            return Channel.SMS;
        // accept plain 10-digit Indian numbers
        if (identifier.matches("^\\d{10}$"))
            return Channel.SMS;
        throw new IllegalArgumentException(
                "Provide a valid email or E.164 phone (e.g. +14155550123) or a 10-digit local phone.");
    }

    // make normalize public so controller can reuse the same canonical form
    public String normalize(String identifier) {
        if (identifier == null)
            throw new IllegalArgumentException("Identifier is required");
        identifier = identifier.trim();
        if (EMAIL_RX.matcher(identifier).matches()) {
            return identifier.toLowerCase();
        }
        if (E164_RX.matcher(identifier).matches()) {
            return identifier;
        }
        if (LOCAL_10_DIGIT.matcher(identifier).matches()) {
            // fallback: assume India +91 for 10-digit local numbers
            return "+91" + identifier;
        }
        // fallback, return trimmed
        return identifier;
    }

    public void sendOtp(String identifier) {
        Channel channel = detectChannel(identifier);
        String normalized = normalize(identifier);

        String otp = generateOtp();
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        OtpData data = new OtpData(otp, expiresAt, 0);
        otpStore.put(normalized, data);

        String body = "Hi there,\n\n"
                + "Welcome to Reeljolly! 🎬\n\n"
                + "Use the verification code below to complete your sign-up or login:\n\n"
                + otp + "\n\n"
                + "This code will expire in " + (ttlSeconds / 60) + " minutes.\n\n"
                + "If you didn’t request this, please ignore this email.\n\n"
                + "— The Reeljolly Team";
                
        try {
            if (channel == Channel.EMAIL) {
                if (emailService == null)
                    throw new IllegalStateException("EmailService is not configured");
                emailService.sendEmail(normalized, "Your Reeljolly Verification Code", body);
            } else {
                if (smsService == null)
                    throw new IllegalStateException("SmsService is not configured");
                smsService.sendSms(normalized, body);
            }
        } catch (IllegalArgumentException iae) {
            // validation-related, bubble up as 400
            throw iae;
        } catch (Exception e) {
            // log provider stacktrace for debugging and rethrow a clear runtime exception
            log.error("Provider send failed for {} : {}", normalized, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP due to provider error", e);
        }

        log.info("[DEBUG] OTP for {} = {}", normalized, otp);
    }

    public boolean verifyOtp(String identifier, String submitted) {
        String normalized = normalize(identifier);
        OtpData data = otpStore.get(normalized);
        if (data == null)
            return false;

        if (Instant.now().isAfter(data.getExpiresAt())) {
            // use normalized key when removing
            otpStore.remove(normalized);
            return false;
        }

        // increment attempts
        data.incrementAttempts();
        if (data.getAttempts() > maxAttempts) {
            otpStore.remove(normalized);
            return false;
        }

        boolean ok = data.getCode().equals(submitted);
        if (ok) {
            otpStore.remove(normalized);
            // mark verified for a short period so subsequent /register call can verify
            // presence
            verifiedStore.put(normalized, Instant.now().plusSeconds(15 * 60)); // 15 minutes
        }
        return ok;
    }

    /**
     * Called by register endpoint to ensure identifier is verified recently
     */
    public boolean isVerified(String identifier) {
        String normalized = normalize(identifier);
        Instant expiry = verifiedStore.get(normalized);
        if (expiry == null)
            return false;
        if (Instant.now().isAfter(expiry)) {
            verifiedStore.remove(normalized);
            return false;
        }
        // Optionally remove after checking to make verification single-use:
        // verifiedStore.remove(normalized);
        return true;
    }

    /**
     * Remove verified marker (call after successful register)
     */
    public void consumeVerified(String identifier) {
        String normalized = normalize(identifier);
        verifiedStore.remove(normalized);
    }
}
