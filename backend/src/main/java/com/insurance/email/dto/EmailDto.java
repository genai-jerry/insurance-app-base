package com.insurance.email.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.insurance.common.entity.EmailLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDto {

    private Long id;
    private Long leadId;
    private String leadName;
    private Long agentId;
    private String agentName;
    private Long prospectusId;
    private String toEmail;
    private String subject;
    private String body;
    private EmailLog.EmailStatus status;
    private String providerMessageId;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
