package com.projects.instagram.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Simple Twilio wrapper. Reads Twilio credentials from properties.
 */
@Service
public class SmsService {


    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Value("${twilio.fromNumber}")
    private String fromNumber;

    //
//    @PostConstruct
//    public void init() {
//        Twilio.init(accountSid, authToken);
//    }
//
//    public void sendSms(String toNumber, String body) {
//        Message.creator(new PhoneNumber(toNumber), new PhoneNumber(fromNumber), body).create();
//    }
    @PostConstruct
    public void init() {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            log.warn("Twilio credentials not configured. SMS will fail until configured.");
            return;
        }
        Twilio.init(accountSid, authToken);
    }


    public void sendSms(String toNumber, String body) {
        try {
            if (fromNumber == null || fromNumber.isBlank())
                throw new IllegalStateException("twilio.fromNumber not configured");
            Message.creator(new PhoneNumber(toNumber), new PhoneNumber(fromNumber), body).create();
            log.info("SMS sent to {}", toNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS to {} : {}", toNumber, e.getMessage(), e);
            throw e;
        }
    }
}
