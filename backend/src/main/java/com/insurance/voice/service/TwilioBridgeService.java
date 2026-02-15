package com.insurance.voice.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;

/**
 * Service to bridge PSTN calls to OpenAI Realtime API via Twilio
 */
@Service
@Slf4j
public class TwilioBridgeService {

    @Value("${app.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.twilio.auth-token:}")
    private String authToken;

    @Value("${app.twilio.phone-number:}")
    private String twilioPhoneNumber;

    @Value("${app.voice.mock-mode:true}")
    private boolean mockMode;

    @PostConstruct
    public void init() {
        if (!mockMode && accountSid != null && !accountSid.isEmpty()) {
            try {
                Twilio.init(accountSid, authToken);
                log.info("Twilio initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio", e);
            }
        }
    }

    /**
     * Initiate an outbound call to a lead
     */
    public String initiateCall(String toPhoneNumber, String websocketUrl) {
        if (mockMode) {
            log.info("Mock mode: Would initiate call to {} with websocket {}", toPhoneNumber, websocketUrl);
            return "mock-call-sid-" + System.currentTimeMillis();
        }

        try {
            // In production, this would create a TwiML app that connects the call
            // to a WebSocket endpoint that bridges to OpenAI Realtime API
            Call call = Call.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                URI.create(websocketUrl)
            ).create();

            log.info("Initiated Twilio call with SID: {}", call.getSid());
            return call.getSid();

        } catch (Exception e) {
            log.error("Failed to initiate Twilio call", e);
            throw new RuntimeException("Failed to initiate call: " + e.getMessage());
        }
    }

    /**
     * End an active call
     */
    public void endCall(String callSid) {
        if (mockMode) {
            log.info("Mock mode: Would end call {}", callSid);
            return;
        }

        try {
            Call.updater(callSid)
                .setStatus(Call.UpdateStatus.COMPLETED)
                .update();

            log.info("Ended Twilio call: {}", callSid);

        } catch (Exception e) {
            log.error("Failed to end Twilio call", e);
        }
    }

    /**
     * Get call status
     */
    public String getCallStatus(String callSid) {
        if (mockMode) {
            return "completed";
        }

        try {
            Call call = Call.fetcher(callSid).fetch();
            return call.getStatus().toString();

        } catch (Exception e) {
            log.error("Failed to fetch call status", e);
            return "unknown";
        }
    }
}
