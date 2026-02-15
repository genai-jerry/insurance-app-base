package com.insurance.prospectus.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateProspectusRequest {

    @NotNull(message = "Lead ID is required")
    private Long leadId;

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    private Long voiceSessionId;

    private List<Long> productIds;

    private Map<String, Object> customerNeeds;

    private String additionalNotes;
}
