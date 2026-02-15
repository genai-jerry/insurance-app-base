package com.insurance.products.dto;

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
public class ProductDto {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String insurer;
    private String planType;
    private Map<String, Object> detailsJson;
    private Map<String, Object> eligibilityJson;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
