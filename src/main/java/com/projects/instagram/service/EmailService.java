package com.projects.instagram.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Tiny wrapper around JavaMailSender for sending simple emails.
 */
@Service
public class EmailService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    // optional default from address if your SMTP provider requires it
    @Value("${spring.mail.from}")
    private String defaultFrom;


    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            if (defaultFrom != null && !defaultFrom.isBlank()) {
                msg.setFrom(defaultFrom);
            }
            mailSender.send(msg);
            log.info("Email queued to {}", to);
        } catch (Exception e) {
// log full stacktrace, rethrow so controller can record/handle it
            log.error("Failed to send email to {} : {}", to, e.getMessage(), e);
            throw e;
        }
    }
}
