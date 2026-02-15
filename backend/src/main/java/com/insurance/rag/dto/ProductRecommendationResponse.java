package com.insurance.rag.dto;

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
public class ProductRecommendationResponse {

    private String narrative;
    private List<RecommendedProduct> products;
    private Map<String, Object> matchedNeeds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedProduct {
        private Long productId;
        private String productName;
        private String insurer;
        private String planType;
        private Double relevanceScore;
        private String reasoning;
        private Map<String, Object> details;
    }
}
