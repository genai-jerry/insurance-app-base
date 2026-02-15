package com.insurance.leads.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.insurance.common.entity.LeadActivity.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeadActivityDto {

    private Long id;
    private Long leadId;
    private ActivityType type;
    private Map<String, Object> payload;
    private LocalDateTime createdAt;
}
