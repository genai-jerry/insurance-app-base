package com.insurance.leads.controller;

import com.insurance.common.entity.Lead.LeadStatus;
import com.insurance.leads.dto.*;
import com.insurance.leads.service.LeadActivityService;
import com.insurance.leads.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Lead management APIs")
@SecurityRequirement(name = "bearerAuth")
public class LeadController {

    private final LeadService leadService;
    private final LeadActivityService leadActivityService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get all leads", description = "Get all leads with optional filters. Agents can only see their assigned leads, admins see all.")
    public ResponseEntity<Page<LeadDto>> getAllLeads(
            @Parameter(description = "Filter by assigned agent ID")
            @RequestParam(required = false) Long agentId,

            @Parameter(description = "Filter by lead status")
            @RequestParam(required = false) LeadStatus status,

            @Parameter(description = "Search by name, phone, or email")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction (asc or desc)")
            @RequestParam(defaultValue = "desc") String sortDir,

            Authentication authentication) {

        // RBAC: If user is an agent, force filter by their ID
        Long effectiveAgentId = agentId;
        if (!isAdmin(authentication)) {
            effectiveAgentId = getUserId(authentication);
            log.debug("Agent {} requesting their leads", effectiveAgentId);
        }

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<LeadDto> leads = leadService.getAllLeads(effectiveAgentId, status, search, pageable);
        return ResponseEntity.ok(leads);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get lead by ID", description = "Get a specific lead by ID. Agents can only view their assigned leads.")
    public ResponseEntity<LeadDto> getLeadById(
            @Parameter(description = "Lead ID")
            @PathVariable Long id,
            Authentication authentication) {

        LeadDto lead = leadService.getLeadById(id);

        // RBAC: Agents can only view their own leads
        if (!isAdmin(authentication) && !lead.getAssignedAgentId().equals(getUserId(authentication))) {
            log.warn("Agent {} attempted to access lead {} which is not assigned to them",
                    getUserId(authentication), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(lead);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new lead", description = "Create a new lead. Admin only.")
    public ResponseEntity<LeadDto> createLead(
            @Valid @RequestBody CreateLeadRequest request) {

        LeadDto createdLead = leadService.createLead(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLead);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Update a lead", description = "Update an existing lead. Agents can only update their assigned leads.")
    public ResponseEntity<LeadDto> updateLead(
            @Parameter(description = "Lead ID")
            @PathVariable Long id,

            @Valid @RequestBody UpdateLeadRequest request,
            Authentication authentication) {

        // RBAC: Check if agent is authorized to update this lead
        if (!isAdmin(authentication)) {
            LeadDto existingLead = leadService.getLeadById(id);
            if (!existingLead.getAssignedAgentId().equals(getUserId(authentication))) {
                log.warn("Agent {} attempted to update lead {} which is not assigned to them",
                        getUserId(authentication), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Agents cannot reassign leads
            if (request.getAssignedAgentId() != null &&
                !request.getAssignedAgentId().equals(getUserId(authentication))) {
                log.warn("Agent {} attempted to reassign lead {}", getUserId(authentication), id);
                request.setAssignedAgentId(null); // Ignore the reassignment
            }
        }

        LeadDto updatedLead = leadService.updateLead(id, request);
        return ResponseEntity.ok(updatedLead);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a lead", description = "Delete a lead. Admin only.")
    public ResponseEntity<Void> deleteLead(
            @Parameter(description = "Lead ID")
            @PathVariable Long id) {

        leadService.deleteLead(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign lead to agent", description = "Assign a lead to a specific agent. Admin only.")
    public ResponseEntity<LeadDto> assignLead(
            @Parameter(description = "Lead ID")
            @PathVariable Long id,

            @Parameter(description = "Agent ID to assign to")
            @RequestParam Long agentId) {

        LeadDto updatedLead = leadService.assignLead(id, agentId);
        return ResponseEntity.ok(updatedLead);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Update lead status", description = "Update the status of a lead. Agents can only update their assigned leads.")
    public ResponseEntity<LeadDto> updateLeadStatus(
            @Parameter(description = "Lead ID")
            @PathVariable Long id,

            @Parameter(description = "New status")
            @RequestParam LeadStatus status,

            Authentication authentication) {

        // RBAC: Check if agent is authorized to update this lead
        if (!isAdmin(authentication)) {
            LeadDto existingLead = leadService.getLeadById(id);
            if (!existingLead.getAssignedAgentId().equals(getUserId(authentication))) {
                log.warn("Agent {} attempted to update status of lead {} which is not assigned to them",
                        getUserId(authentication), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        LeadDto updatedLead = leadService.updateLeadStatus(id, status);

        // Log status change activity
        leadActivityService.logStatusChange(id,
                updatedLead.getStatus().toString(),
                status.toString());

        return ResponseEntity.ok(updatedLead);
    }

    @GetMapping("/{id}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get lead activities", description = "Get activity timeline for a lead. Agents can only view activities for their assigned leads.")
    public ResponseEntity<List<LeadActivityDto>> getLeadActivities(
            @Parameter(description = "Lead ID")
            @PathVariable Long id,
            Authentication authentication) {

        // RBAC: Check if agent is authorized to view this lead's activities
        if (!isAdmin(authentication)) {
            LeadDto lead = leadService.getLeadById(id);
            if (!lead.getAssignedAgentId().equals(getUserId(authentication))) {
                log.warn("Agent {} attempted to view activities for lead {} which is not assigned to them",
                        getUserId(authentication), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        List<LeadActivityDto> activities = leadActivityService.getLeadActivities(id);
        return ResponseEntity.ok(activities);
    }

    @PostMapping("/{id}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Create lead activity", description = "Log a new activity for a lead. Agents can only create activities for their assigned leads.")
    public ResponseEntity<LeadActivityDto> createLeadActivity(
            @Parameter(description = "Lead ID")
            @PathVariable Long id,

            @Valid @RequestBody CreateLeadActivityRequest request,
            Authentication authentication) {

        // RBAC: Check if agent is authorized to create activity for this lead
        if (!isAdmin(authentication)) {
            LeadDto lead = leadService.getLeadById(id);
            if (!lead.getAssignedAgentId().equals(getUserId(authentication))) {
                log.warn("Agent {} attempted to create activity for lead {} which is not assigned to them",
                        getUserId(authentication), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        LeadActivityDto activity = leadActivityService.createLeadActivity(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(activity);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Import leads from CSV", description = "Bulk import leads from a CSV file. Admin only.")
    public ResponseEntity<Map<String, Object>> importLeads(
            @Parameter(description = "CSV file containing leads")
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Default agent ID to assign imported leads to")
            @RequestParam(required = false) Long defaultAgentId) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "CSV file is empty"));
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File must be a CSV"));
            }

            List<LeadDto> importedLeads = leadService.importLeadsFromCsv(file, defaultAgentId);

            Map<String, Object> response = Map.of(
                    "message", "Leads imported successfully",
                    "count", importedLeads.size(),
                    "leads", importedLeads
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            log.error("Error importing leads from CSV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error importing CSV: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get lead statistics", description = "Get statistics about leads. Agents see stats for their assigned leads only.")
    public ResponseEntity<Map<String, Object>> getLeadStats(Authentication authentication) {
        Long agentId = isAdmin(authentication) ? null : getUserId(authentication);

        // This is a simple example - you can expand this with more detailed stats
        Map<String, Object> stats = Map.of(
                "message", "Lead statistics endpoint",
                "agentId", agentId != null ? agentId : "all"
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Helper method to check if user is admin
     */
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    /**
     * Helper method to get user ID from authentication
     * This assumes the authentication principal contains user ID
     * Adjust based on your actual authentication implementation
     */
    private Long getUserId(Authentication authentication) {
        // This is a placeholder - implement based on your auth setup
        // For example, if you're using a custom UserDetails implementation:
        // CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        // return userDetails.getUserId();

        // For now, returning a mock value - replace with actual implementation
        Object principal = authentication.getPrincipal();
        if (principal instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) principal;
            Object userId = userMap.get("userId");
            if (userId instanceof Long) {
                return (Long) userId;
            } else if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
        }

        // Fallback - you should implement proper user ID extraction
        log.warn("Unable to extract user ID from authentication principal");
        return null;
    }
}
