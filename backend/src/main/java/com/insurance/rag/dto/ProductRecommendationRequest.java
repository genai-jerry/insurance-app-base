package com.insurance.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecommendationRequest {

    @NotBlank(message = "Query is required")
    private String query;

    private Map<String, Object> customerNeeds;

    private Integer maxResults;

    private Long leadId;

    private Long voiceSessionId;
}
