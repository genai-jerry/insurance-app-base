package com.insurance.email.service;

import com.insurance.common.entity.EmailLog;
import com.insurance.common.entity.Lead;
import com.insurance.common.entity.Prospectus;
import com.insurance.common.entity.User;
import com.insurance.email.dto.EmailDto;
import com.insurance.email.dto.SendEmailRequest;
import com.insurance.email.repository.EmailLogRepository;
import com.insurance.leads.repository.LeadRepository;
import com.insurance.auth.repository.UserRepository;
import com.insurance.prospectus.repository.ProspectusRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailLogRepository emailLogRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final ProspectusRepository prospectusRepository;
    private final EmailTemplateService templateService;
    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@insurance-app.com}")
    private String fromEmail;

    @Transactional
    public EmailDto sendEmail(SendEmailRequest request) {
        log.info("Sending email to {} for lead {}", request.getToEmail(), request.getLeadId());

        Lead lead = leadRepository.findById(request.getLeadId())
            .orElseThrow(() -> new RuntimeException("Lead not found with id: " + request.getLeadId()));

        User agent = userRepository.findById(request.getAgentId())
            .orElseThrow(() -> new RuntimeException("Agent not found with id: " + request.getAgentId()));

        Prospectus prospectus = null;
        if (request.getProspectusId() != null) {
            prospectus = prospectusRepository.findById(request.getProspectusId())
                .orElseThrow(() -> new RuntimeException("Prospectus not found with id: " + request.getProspectusId()));
        }

        // Create email log
        EmailLog emailLog = EmailLog.builder()
            .lead(lead)
            .agent(agent)
            .prospectus(prospectus)
            .toEmail(request.getToEmail())
            .subject(request.getSubject())
            .body(request.getBody())
            .status(EmailLog.EmailStatus.PENDING)
            .build();

        EmailLog saved = emailLogRepository.save(emailLog);

        // Send email
        try {
            String messageId = sendEmailInternal(
                request.getToEmail(),
                request.getSubject(),
                request.getBody(),
                prospectus,
                Boolean.TRUE.equals(request.getAttachProspectus())
            );

            saved.setStatus(EmailLog.EmailStatus.SENT);
            saved.setProviderMessageId(messageId);
            saved.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Failed to send email", e);
            saved.setStatus(EmailLog.EmailStatus.FAILED);
            saved.setErrorMessage(e.getMessage());
        }

        EmailLog updated = emailLogRepository.save(saved);
        log.info("Email sent with status: {}", updated.getStatus());

        return mapToDto(updated);
    }

    @Transactional
    public EmailDto sendProspectusEmail(Long leadId, Long agentId, Long prospectusId) {
        Lead lead = leadRepository.findById(leadId)
            .orElseThrow(() -> new RuntimeException("Lead not found with id: " + leadId));

        User agent = userRepository.findById(agentId)
            .orElseThrow(() -> new RuntimeException("Agent not found with id: " + agentId));

        Prospectus prospectus = prospectusRepository.findById(prospectusId)
            .orElseThrow(() -> new RuntimeException("Prospectus not found with id: " + prospectusId));

        String subject = templateService.generateProspectusSubject(lead);
        String body = templateService.generateProspectusEmail(lead, agent, prospectus);

        SendEmailRequest request = SendEmailRequest.builder()
            .leadId(leadId)
            .agentId(agentId)
            .toEmail(lead.getEmail())
            .subject(subject)
            .body(body)
            .prospectusId(prospectusId)
            .attachProspectus(true)
            .build();

        return sendEmail(request);
    }

    @Transactional(readOnly = true)
    public EmailDto getEmail(Long id) {
        EmailLog emailLog = emailLogRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Email log not found with id: " + id));
        return mapToDto(emailLog);
    }

    @Transactional(readOnly = true)
    public List<EmailDto> getEmailsByLead(Long leadId) {
        return emailLogRepository.findByLeadId(leadId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmailDto> getEmailsByAgent(Long agentId) {
        return emailLogRepository.findByAgentId(agentId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmailDto> getPendingEmails() {
        return emailLogRepository.findByStatus(EmailLog.EmailStatus.PENDING).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * Internal method to send email using Spring Mail
     */
    private String sendEmailInternal(
        String toEmail,
        String subject,
        String body,
        Prospectus prospectus,
        boolean attachProspectus
    ) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, false);

        // Attach prospectus PDF if requested
        if (attachProspectus && prospectus != null && prospectus.getPdfPath() != null) {
            try {
                byte[] pdfContent = Files.readAllBytes(Paths.get(prospectus.getPdfPath()));
                ByteArrayResource pdfResource = new ByteArrayResource(pdfContent);

                helper.addAttachment(
                    "prospectus_" + prospectus.getId() + ".pdf",
                    pdfResource,
                    "application/pdf"
                );
            } catch (Exception e) {
                log.error("Failed to attach prospectus PDF", e);
            }
        }

        mailSender.send(message);

        // Return message ID (in real implementation, extract from sent message)
        return "msg-" + System.currentTimeMillis();
    }

    private EmailDto mapToDto(EmailLog emailLog) {
        return EmailDto.builder()
            .id(emailLog.getId())
            .leadId(emailLog.getLead().getId())
            .leadName(emailLog.getLead().getName())
            .agentId(emailLog.getAgent().getId())
            .agentName(emailLog.getAgent().getName())
            .prospectusId(emailLog.getProspectus() != null ? emailLog.getProspectus().getId() : null)
            .toEmail(emailLog.getToEmail())
            .subject(emailLog.getSubject())
            .body(emailLog.getBody())
            .status(emailLog.getStatus())
            .providerMessageId(emailLog.getProviderMessageId())
            .errorMessage(emailLog.getErrorMessage())
            .sentAt(emailLog.getSentAt())
            .createdAt(emailLog.getCreatedAt())
            .build();
    }
}
