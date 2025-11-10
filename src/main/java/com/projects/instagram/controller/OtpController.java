package com.projects.instagram.controller;

import com.projects.instagram.entity.User;
import com.projects.instagram.repository.UserReposotory;
import com.projects.instagram.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "http://localhost:5173",
        "http://10.91.2.29:5173",
        "http://10.91.2.29:5173/instagram-clone",
        "*"
})
@RequestMapping("/api/otp")
@RestController
@RequiredArgsConstructor
public class OtpController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OtpController.class);
    private final OtpService otpService;
    private final UserReposotory userRepository;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> send(@RequestBody Map<String, String> body) {

        String identifier = body.get("identifier");
        String username = body.get("username");

        if (identifier == null || identifier.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "identifier is required"));
        }
        // If username provided, ensure it's free
        if (username != null && !username.isBlank()) {
            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("success", "false", "message", "username taken"));
            }
        }

        // detect email vs mobile
        boolean looksLikeEmail = identifier.contains("@");
        String maskedIdentifier;

        try {
            // use service normalization so controller and service use same canonical key
            String normalized = otpService.normalize(identifier);

            if (looksLikeEmail) {
                int atIndex = identifier.indexOf("@");
                String emailName = identifier.substring(0, atIndex);
                String domain = identifier.substring(atIndex);

                // Mask everything after first 3 chars in the email name
                if (emailName.length() > 3) {
                    maskedIdentifier = emailName.substring(0, 3) + "********" + domain;
                } else {
                    maskedIdentifier = emailName.charAt(0) + "********" + domain;
                }

                if (userRepository.findByEmail(normalized).isPresent()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", "false",
                            "message", "email already used"));
                }
            } else {
                int len = identifier.length();
                if (len > 2) {
                    maskedIdentifier = "********" + identifier.substring(len - 2);
                } else {
                    maskedIdentifier = "********";
                }
                // if (userRepository.findByMobileNumber(normalized).isPresent()) {
                // return ResponseEntity.badRequest().body(Map.of("message", "mobile number
                // already used"));
                // }
            }

            // pass the original/raw identifier is fine (service normalizes internally), but
            // passing normalized keeps logs clean
            otpService.sendOtp(normalized);
            
            String successMsg = looksLikeEmail
                    ? "OTP sent to " + maskedIdentifier
                    : "OTP sent to " + maskedIdentifier;
            return ResponseEntity.ok(Map.of("success", "true", "message", successMsg));

        } catch (IllegalArgumentException ex) {
            // validation problems (bad identifier pattern etc.)
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            // log the full stacktrace for debugging
            log.error("Failed to send OTP for identifier='{}'. Cause: {}", identifier, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("message", "failed to send OTP"));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        String identifier = body.get("identifier");
        String otp = body.get("otp");
        if (identifier == null || otp == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", "false", "message", "identifier and otp required"));
        }

        boolean ok = otpService.verifyOtp(identifier, otp);
        if (ok) {
            return ResponseEntity.ok(Map.of("success", "true", "message", "Verified successfully"));
        } else {
            return ResponseEntity.status(400).body(Map.of("success", "false", "message", "Invalid or expired OTP"));
        }
    }

    // NOTE: small helper in controller just for checking lookup. It's optional and
    // mirrors normalization in service.
    private String normalizeForLookup(String identifier) {
        if (identifier == null)
            return null;
        identifier = identifier.trim();
        if (identifier.startsWith("+"))
            return identifier;
        if (identifier.matches("^\\d{10}$"))
            return "+91" + identifier; // same default used in service
        return identifier;
    }
}
