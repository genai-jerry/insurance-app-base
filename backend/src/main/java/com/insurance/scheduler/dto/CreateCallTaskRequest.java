package com.insurance.scheduler.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCallTaskRequest {

    @NotNull(message = "Lead ID is required")
    private Long leadId;

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledTime;

    private String notes;

    private Boolean usePreferredTimeWindow;
}
