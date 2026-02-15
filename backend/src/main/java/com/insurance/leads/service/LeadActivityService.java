package com.insurance.leads.service;

import com.insurance.common.entity.Lead;
import com.insurance.common.entity.LeadActivity;
import com.insurance.common.entity.LeadActivity.ActivityType;
import com.insurance.leads.dto.CreateLeadActivityRequest;
import com.insurance.leads.dto.LeadActivityDto;
import com.insurance.leads.repository.LeadActivityRepository;
import com.insurance.leads.repository.LeadRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadActivityService {

    private final LeadActivityRepository leadActivityRepository;
    private final LeadRepository leadRepository;

    /**
     * Get all activities for a lead (timeline)
     */
    @Transactional(readOnly = true)
    public List<LeadActivityDto> getLeadActivities(Long leadId) {
        if (!leadRepository.existsById(leadId)) {
            throw new EntityNotFoundException("Lead not found with id: " + leadId);
        }

        List<LeadActivity> activities = leadActivityRepository.findByLeadIdOrderByCreatedAtDesc(leadId);
        return activities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Create a new activity for a lead
     */
    @Transactional
    public LeadActivityDto createLeadActivity(Long leadId, CreateLeadActivityRequest request) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with id: " + leadId));

        LeadActivity activity = LeadActivity.builder()
                .lead(lead)
                .type(request.getType())
                .payload(request.getPayload())
                .build();

        LeadActivity savedActivity = leadActivityRepository.save(activity);
        log.info("Created activity {} for lead {}", savedActivity.getId(), leadId);
        return convertToDto(savedActivity);
    }

    /**
     * Log a note activity
     */
    @Transactional
    public LeadActivityDto logNote(Long leadId, String note) {
        CreateLeadActivityRequest request = CreateLeadActivityRequest.builder()
                .type(ActivityType.NOTE)
                .payload(Map.of("note", note))
                .build();
        return createLeadActivity(leadId, request);
    }

    /**
     * Log a call activity
     */
    @Transactional
    public LeadActivityDto logCall(Long leadId, Map<String, Object> callDetails) {
        CreateLeadActivityRequest request = CreateLeadActivityRequest.builder()
                .type(ActivityType.CALL)
                .payload(callDetails)
                .build();
        return createLeadActivity(leadId, request);
    }

    /**
     * Log an email activity
     */
    @Transactional
    public LeadActivityDto logEmail(Long leadId, Map<String, Object> emailDetails) {
        CreateLeadActivityRequest request = CreateLeadActivityRequest.builder()
                .type(ActivityType.EMAIL)
                .payload(emailDetails)
                .build();
        return createLeadActivity(leadId, request);
    }

    /**
     * Log a status change activity
     */
    @Transactional
    public LeadActivityDto logStatusChange(Long leadId, String oldStatus, String newStatus) {
        CreateLeadActivityRequest request = CreateLeadActivityRequest.builder()
                .type(ActivityType.STATUS_CHANGE)
                .payload(Map.of(
                        "oldStatus", oldStatus,
                        "newStatus", newStatus
                ))
                .build();
        return createLeadActivity(leadId, request);
    }

    /**
     * Log a prospectus sent activity
     */
    @Transactional
    public LeadActivityDto logProspectusSent(Long leadId, Map<String, Object> prospectusDetails) {
        CreateLeadActivityRequest request = CreateLeadActivityRequest.builder()
                .type(ActivityType.PROSPECTUS_SENT)
                .payload(prospectusDetails)
                .build();
        return createLeadActivity(leadId, request);
    }

    /**
     * Get activity count by type
     */
    @Transactional(readOnly = true)
    public Long getActivityCount(Long leadId, ActivityType type) {
        if (!leadRepository.existsById(leadId)) {
            throw new EntityNotFoundException("Lead not found with id: " + leadId);
        }
        return leadActivityRepository.countByLeadIdAndType(leadId, type);
    }

    /**
     * Get total activity count
     */
    @Transactional(readOnly = true)
    public Long getTotalActivityCount(Long leadId) {
        if (!leadRepository.existsById(leadId)) {
            throw new EntityNotFoundException("Lead not found with id: " + leadId);
        }
        return leadActivityRepository.countByLeadId(leadId);
    }

    /**
     * Convert LeadActivity entity to DTO
     */
    private LeadActivityDto convertToDto(LeadActivity activity) {
        return LeadActivityDto.builder()
                .id(activity.getId())
                .leadId(activity.getLead().getId())
                .type(activity.getType())
                .payload(activity.getPayload())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
