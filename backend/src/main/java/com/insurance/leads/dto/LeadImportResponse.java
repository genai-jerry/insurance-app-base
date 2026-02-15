package com.insurance.leads.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadImportResponse {

    private String message;
    private int totalCount;
    private int successCount;
    private int failureCount;
    private List<LeadDto> importedLeads;
    private List<String> errors;
}
