package com.insurance.leads.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadImportRequest {

    @NotNull(message = "CSV file is required")
    private MultipartFile file;

    private Long defaultAssignedAgentId;
}
