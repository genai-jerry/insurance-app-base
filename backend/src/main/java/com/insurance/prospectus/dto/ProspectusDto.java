package com.insurance.prospectus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProspectusDto {

    private Long id;
    private Long leadId;
    private String leadName;
    private Long agentId;
    private String agentName;
    private Long voiceSessionId;
    private Integer version;
    private String htmlContent;
    private String pdfPath;
    private String pdfUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
