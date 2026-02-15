package com.insurance.leads.service;

import com.insurance.common.entity.Lead;
import com.insurance.common.entity.Lead.LeadStatus;
import com.insurance.common.entity.User;
import com.insurance.leads.dto.CreateLeadRequest;
import com.insurance.leads.dto.LeadDto;
import com.insurance.leads.dto.UpdateLeadRequest;
import com.insurance.leads.repository.LeadRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;

    /**
     * Get all leads with optional filters
     */
    @Transactional(readOnly = true)
    public Page<LeadDto> getAllLeads(Long agentId, LeadStatus status, String search, Pageable pageable) {
        Page<Lead> leads;

        if (agentId != null && status != null && search != null && !search.isEmpty()) {
            leads = leadRepository.searchLeadsByAgentAndStatus(agentId, status, search, pageable);
        } else if (agentId != null && status != null) {
            leads = leadRepository.findByAssignedAgentIdAndStatus(agentId, status, pageable);
        } else if (agentId != null && search != null && !search.isEmpty()) {
            leads = leadRepository.searchLeadsByAgent(agentId, search, pageable);
        } else if (status != null && search != null && !search.isEmpty()) {
            leads = leadRepository.searchLeadsByStatus(status, search, pageable);
        } else if (agentId != null) {
            leads = leadRepository.findByAssignedAgentId(agentId, pageable);
        } else if (status != null) {
            leads = leadRepository.findByStatus(status, pageable);
        } else if (search != null && !search.isEmpty()) {
            leads = leadRepository.searchLeads(search, pageable);
        } else {
            leads = leadRepository.findAll(pageable);
        }

        return leads.map(this::convertToDto);
    }

    /**
     * Get lead by ID
     */
    @Transactional(readOnly = true)
    public LeadDto getLeadById(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with id: " + id));
        return convertToDto(lead);
    }

    /**
     * Create a new lead
     */
    @Transactional
    public LeadDto createLead(CreateLeadRequest request) {
        Lead lead = Lead.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .location(request.getLocation())
                .age(request.getAge())
                .incomeBand(request.getIncomeBand())
                .leadSource(request.getLeadSource())
                .status(request.getStatus() != null ? request.getStatus() : LeadStatus.NEW)
                .preferredTimeWindows(request.getPreferredTimeWindows())
                .timezone(request.getTimezone())
                .consentFlags(request.getConsentFlags())
                .notes(request.getNotes())
                .build();

        if (request.getAssignedAgentId() != null) {
            User agent = new User();
            agent.setId(request.getAssignedAgentId());
            lead.setAssignedAgent(agent);
        }

        Lead savedLead = leadRepository.save(lead);
        log.info("Created new lead with id: {}", savedLead.getId());
        return convertToDto(savedLead);
    }

    /**
     * Update an existing lead
     */
    @Transactional
    public LeadDto updateLead(Long id, UpdateLeadRequest request) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with id: " + id));

        if (request.getName() != null) {
            lead.setName(request.getName());
        }
        if (request.getPhone() != null) {
            lead.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            lead.setEmail(request.getEmail());
        }
        if (request.getLocation() != null) {
            lead.setLocation(request.getLocation());
        }
        if (request.getAge() != null) {
            lead.setAge(request.getAge());
        }
        if (request.getIncomeBand() != null) {
            lead.setIncomeBand(request.getIncomeBand());
        }
        if (request.getLeadSource() != null) {
            lead.setLeadSource(request.getLeadSource());
        }
        if (request.getStatus() != null) {
            lead.setStatus(request.getStatus());
        }
        if (request.getPreferredTimeWindows() != null) {
            lead.setPreferredTimeWindows(request.getPreferredTimeWindows());
        }
        if (request.getTimezone() != null) {
            lead.setTimezone(request.getTimezone());
        }
        if (request.getConsentFlags() != null) {
            lead.setConsentFlags(request.getConsentFlags());
        }
        if (request.getNotes() != null) {
            lead.setNotes(request.getNotes());
        }
        if (request.getAssignedAgentId() != null) {
            User agent = new User();
            agent.setId(request.getAssignedAgentId());
            lead.setAssignedAgent(agent);
        }

        Lead updatedLead = leadRepository.save(lead);
        log.info("Updated lead with id: {}", updatedLead.getId());
        return convertToDto(updatedLead);
    }

    /**
     * Delete a lead
     */
    @Transactional
    public void deleteLead(Long id) {
        if (!leadRepository.existsById(id)) {
            throw new EntityNotFoundException("Lead not found with id: " + id);
        }
        leadRepository.deleteById(id);
        log.info("Deleted lead with id: {}", id);
    }

    /**
     * Assign lead to an agent
     */
    @Transactional
    public LeadDto assignLead(Long leadId, Long agentId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with id: " + leadId));

        User agent = new User();
        agent.setId(agentId);
        lead.setAssignedAgent(agent);

        Lead updatedLead = leadRepository.save(lead);
        log.info("Assigned lead {} to agent {}", leadId, agentId);
        return convertToDto(updatedLead);
    }

    /**
     * Update lead status
     */
    @Transactional
    public LeadDto updateLeadStatus(Long leadId, LeadStatus status) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with id: " + leadId));

        lead.setStatus(status);
        Lead updatedLead = leadRepository.save(lead);
        log.info("Updated lead {} status to {}", leadId, status);
        return convertToDto(updatedLead);
    }

    /**
     * Import leads from CSV file
     */
    @Transactional
    public List<LeadDto> importLeadsFromCsv(MultipartFile file, Long defaultAgentId) throws IOException {
        List<LeadDto> importedLeads = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // First row is header
            String[] headers = rows.get(0);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].toLowerCase().trim(), i);
            }

            // Process data rows
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                try {
                    Lead lead = parseCsvRow(row, headerIndex, defaultAgentId);
                    Lead savedLead = leadRepository.save(lead);
                    importedLeads.add(convertToDto(savedLead));
                } catch (Exception e) {
                    log.error("Error importing row {}: {}", i + 1, e.getMessage());
                }
            }

            log.info("Imported {} leads from CSV", importedLeads.size());
            return importedLeads;

        } catch (CsvException e) {
            log.error("Error parsing CSV file", e);
            throw new IOException("Error parsing CSV file: " + e.getMessage(), e);
        }
    }

    /**
     * Parse a CSV row into a Lead entity
     */
    private Lead parseCsvRow(String[] row, Map<String, Integer> headerIndex, Long defaultAgentId) {
        Lead.LeadBuilder builder = Lead.builder();

        // Required fields
        builder.name(getColumnValue(row, headerIndex, "name"));
        builder.phone(getColumnValue(row, headerIndex, "phone"));

        // Optional fields
        String email = getColumnValue(row, headerIndex, "email");
        if (email != null && !email.isEmpty()) {
            builder.email(email);
        }

        String location = getColumnValue(row, headerIndex, "location");
        if (location != null && !location.isEmpty()) {
            builder.location(location);
        }

        String ageStr = getColumnValue(row, headerIndex, "age");
        if (ageStr != null && !ageStr.isEmpty()) {
            try {
                builder.age(Integer.parseInt(ageStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid age value: {}", ageStr);
            }
        }

        String incomeBand = getColumnValue(row, headerIndex, "income_band");
        if (incomeBand != null && !incomeBand.isEmpty()) {
            builder.incomeBand(incomeBand);
        }

        String leadSource = getColumnValue(row, headerIndex, "lead_source");
        if (leadSource != null && !leadSource.isEmpty()) {
            builder.leadSource(leadSource);
        }

        String statusStr = getColumnValue(row, headerIndex, "status");
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                builder.status(LeadStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                builder.status(LeadStatus.NEW);
            }
        } else {
            builder.status(LeadStatus.NEW);
        }

        String timezone = getColumnValue(row, headerIndex, "timezone");
        if (timezone != null && !timezone.isEmpty()) {
            builder.timezone(timezone);
        }

        String notes = getColumnValue(row, headerIndex, "notes");
        if (notes != null && !notes.isEmpty()) {
            builder.notes(notes);
        }

        Lead lead = builder.build();

        // Set assigned agent
        String agentIdStr = getColumnValue(row, headerIndex, "assigned_agent_id");
        if (agentIdStr != null && !agentIdStr.isEmpty()) {
            try {
                User agent = new User();
                agent.setId(Long.parseLong(agentIdStr));
                lead.setAssignedAgent(agent);
            } catch (NumberFormatException e) {
                log.warn("Invalid agent ID: {}", agentIdStr);
            }
        } else if (defaultAgentId != null) {
            User agent = new User();
            agent.setId(defaultAgentId);
            lead.setAssignedAgent(agent);
        }

        return lead;
    }

    /**
     * Get column value from CSV row
     */
    private String getColumnValue(String[] row, Map<String, Integer> headerIndex, String columnName) {
        Integer index = headerIndex.get(columnName);
        if (index != null && index < row.length) {
            String value = row[index].trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    /**
     * Convert Lead entity to DTO
     */
    private LeadDto convertToDto(Lead lead) {
        return LeadDto.builder()
                .id(lead.getId())
                .assignedAgentId(lead.getAssignedAgent() != null ? lead.getAssignedAgent().getId() : null)
                .assignedAgentName(lead.getAssignedAgent() != null ? lead.getAssignedAgent().getName() : null)
                .name(lead.getName())
                .phone(lead.getPhone())
                .email(lead.getEmail())
                .location(lead.getLocation())
                .age(lead.getAge())
                .incomeBand(lead.getIncomeBand())
                .leadSource(lead.getLeadSource())
                .status(lead.getStatus())
                .preferredTimeWindows(lead.getPreferredTimeWindows())
                .timezone(lead.getTimezone())
                .consentFlags(lead.getConsentFlags())
                .notes(lead.getNotes())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .build();
    }
}
