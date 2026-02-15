package com.insurance.leads.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.insurance.common.entity.Lead.LeadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeadDto {

    private Long id;
    private Long assignedAgentId;
    private String assignedAgentName;
    private String name;
    private String phone;
    private String email;
    private String location;
    private Integer age;
    private String incomeBand;
    private String leadSource;
    private LeadStatus status;
    private List<Map<String, Object>> preferredTimeWindows;
    private String timezone;
    private Map<String, Object> consentFlags;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
