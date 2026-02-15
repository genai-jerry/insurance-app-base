package com.insurance.leads.dto;

import com.insurance.common.entity.Lead.LeadStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class CreateLeadRequest {

    private Long assignedAgentId;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone must be a valid phone number")
    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    private Integer age;

    @Size(max = 50, message = "Income band must not exceed 50 characters")
    private String incomeBand;

    @Size(max = 100, message = "Lead source must not exceed 100 characters")
    private String leadSource;

    private LeadStatus status;

    private List<Map<String, Object>> preferredTimeWindows;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    private Map<String, Object> consentFlags;

    private String notes;
}
