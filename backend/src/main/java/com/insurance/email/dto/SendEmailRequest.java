package com.insurance.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {

    @NotNull(message = "Lead ID is required")
    private Long leadId;

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotBlank(message = "To email is required")
    @Email(message = "Invalid email format")
    private String toEmail;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Body is required")
    private String body;

    private Long prospectusId;

    private Boolean attachProspectus;
}
