package com.insurance.email.controller;

import com.insurance.email.dto.EmailDto;
import com.insurance.email.dto.SendEmailRequest;
import com.insurance.email.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Endpoints for sending and managing emails")
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Send an email")
    public ResponseEntity<EmailDto> sendEmail(@Valid @RequestBody SendEmailRequest request) {
        EmailDto email = emailService.sendEmail(request);
        return ResponseEntity.ok(email);
    }

    @PostMapping("/send-prospectus")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Send prospectus email with PDF attachment")
    public ResponseEntity<EmailDto> sendProspectusEmail(
        @RequestParam Long leadId,
        @RequestParam Long agentId,
        @RequestParam Long prospectusId
    ) {
        EmailDto email = emailService.sendProspectusEmail(leadId, agentId, prospectusId);
        return ResponseEntity.ok(email);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get email by ID")
    public ResponseEntity<EmailDto> getEmail(@PathVariable Long id) {
        EmailDto email = emailService.getEmail(id);
        return ResponseEntity.ok(email);
    }

    @GetMapping("/lead/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get email history for a lead")
    public ResponseEntity<List<EmailDto>> getEmailsByLead(@PathVariable Long leadId) {
        List<EmailDto> emails = emailService.getEmailsByLead(leadId);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get emails sent by an agent")
    public ResponseEntity<List<EmailDto>> getEmailsByAgent(@PathVariable Long agentId) {
        List<EmailDto> emails = emailService.getEmailsByAgent(agentId);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all pending emails")
    public ResponseEntity<List<EmailDto>> getPendingEmails() {
        List<EmailDto> emails = emailService.getPendingEmails();
        return ResponseEntity.ok(emails);
    }
}
