package com.insurance.voice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartVoiceSessionRequest {

    @NotNull(message = "Lead ID is required")
    private Long leadId;

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    private Long callTaskId;

    private String phoneNumber;

    private Boolean mockMode;
}
