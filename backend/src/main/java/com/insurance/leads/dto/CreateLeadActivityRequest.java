package com.insurance.leads.dto;

import com.insurance.common.entity.LeadActivity.ActivityType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeadActivityRequest {

    @NotNull(message = "Activity type is required")
    private ActivityType type;

    private Map<String, Object> payload;
}
