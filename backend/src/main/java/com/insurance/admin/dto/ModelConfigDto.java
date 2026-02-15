package com.insurance.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigDto {

    private String textModel;
    private String embeddingModel;
    private String realtimeModel;
    private Double temperature;
    private Integer maxTokens;
}
