package com.insurance.scheduler.service;

import com.insurance.common.entity.CallTask;
import com.insurance.common.entity.Lead;
import com.insurance.common.entity.User;
import com.insurance.leads.repository.LeadRepository;
import com.insurance.auth.repository.UserRepository;
import com.insurance.scheduler.dto.CallTaskDto;
import com.insurance.scheduler.dto.CreateCallTaskRequest;
import com.insurance.scheduler.dto.UpdateCallTaskRequest;
import com.insurance.scheduler.repository.CallTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallTaskService {

    private final CallTaskRepository callTaskRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;

    @Transactional
    public CallTaskDto createCallTask(CreateCallTaskRequest request) {
        Lead lead = leadRepository.findById(request.getLeadId())
            .orElseThrow(() -> new RuntimeException("Lead not found with id: " + request.getLeadId()));

        User agent = userRepository.findById(request.getAgentId())
            .orElseThrow(() -> new RuntimeException("Agent not found with id: " + request.getAgentId()));

        LocalDateTime scheduledTime = request.getScheduledTime();

        // If usePreferredTimeWindow is true and no specific time provided, calculate optimal time
        if (Boolean.TRUE.equals(request.getUsePreferredTimeWindow()) && scheduledTime == null) {
            scheduledTime = calculateOptimalCallTime(lead);
        }

        // Default to next business day 10 AM if no time specified
        if (scheduledTime == null) {
            scheduledTime = getNextBusinessDayMorning();
        }

        CallTask callTask = CallTask.builder()
            .lead(lead)
            .agent(agent)
            .scheduledTime(scheduledTime)
            .status(CallTask.TaskStatus.PENDING)
            .notes(request.getNotes())
            .build();

        CallTask saved = callTaskRepository.save(callTask);
        log.info("Created call task {} for lead {} with agent {}", saved.getId(), lead.getId(), agent.getId());

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public CallTaskDto getCallTask(Long id) {
        CallTask callTask = callTaskRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Call task not found with id: " + id));
        return mapToDto(callTask);
    }

    @Transactional(readOnly = true)
    public List<CallTaskDto> getCallTasksByAgent(Long agentId) {
        return callTaskRepository.findByAgentId(agentId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CallTaskDto> getCallTasksByLead(Long leadId) {
        return callTaskRepository.findByLeadId(leadId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CallTaskDto> getCallTasksByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return callTaskRepository.findByScheduledTimeBetween(startTime, endTime).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CallTaskDto> getCallTasksByAgentAndDateRange(Long agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return callTaskRepository.findByAgentIdAndScheduledTimeBetween(agentId, startTime, endTime).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CallTaskDto> getPendingCallTasks() {
        return callTaskRepository.findByStatus(CallTask.TaskStatus.PENDING).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public CallTaskDto updateCallTask(Long id, UpdateCallTaskRequest request) {
        CallTask callTask = callTaskRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Call task not found with id: " + id));

        if (request.getScheduledTime() != null) {
            callTask.setScheduledTime(request.getScheduledTime());
        }

        if (request.getStatus() != null) {
            callTask.setStatus(request.getStatus());
            if (request.getStatus() == CallTask.TaskStatus.DONE) {
                callTask.setCompletedAt(LocalDateTime.now());
            }
        }

        if (request.getOutcome() != null) {
            callTask.setOutcome(request.getOutcome());
        }

        if (request.getNotes() != null) {
            callTask.setNotes(request.getNotes());
        }

        CallTask updated = callTaskRepository.save(callTask);
        log.info("Updated call task {}", id);

        return mapToDto(updated);
    }

    @Transactional
    public CallTaskDto markComplete(Long id, String outcome, String notes) {
        CallTask callTask = callTaskRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Call task not found with id: " + id));

        callTask.setStatus(CallTask.TaskStatus.DONE);
        callTask.setOutcome(outcome);
        callTask.setNotes(notes);
        callTask.setCompletedAt(LocalDateTime.now());

        CallTask updated = callTaskRepository.save(callTask);
        log.info("Marked call task {} as complete with outcome: {}", id, outcome);

        return mapToDto(updated);
    }

    @Transactional
    public void deleteCallTask(Long id) {
        if (!callTaskRepository.existsById(id)) {
            throw new RuntimeException("Call task not found with id: " + id);
        }
        callTaskRepository.deleteById(id);
        log.info("Deleted call task {}", id);
    }

    /**
     * Calculate optimal call time based on lead's preferred time windows
     */
    private LocalDateTime calculateOptimalCallTime(Lead lead) {
        List<Map<String, Object>> preferredTimeWindows = lead.getPreferredTimeWindows();

        if (preferredTimeWindows == null || preferredTimeWindows.isEmpty()) {
            return getNextBusinessDayMorning();
        }

        // Use the first available time window's start time
        Map<String, Object> firstWindow = preferredTimeWindows.get(0);
        String startTime = (String) firstWindow.get("start");
        if (startTime != null) {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            LocalTime time = LocalTime.parse(startTime);
            return LocalDateTime.of(tomorrow, time);
        }

        return getNextBusinessDayMorning();
    }

    /**
     * Get next business day at 10 AM
     */
    private LocalDateTime getNextBusinessDayMorning() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Skip weekends
        while (tomorrow.getDayOfWeek().getValue() >= 6) {
            tomorrow = tomorrow.plusDays(1);
        }

        return LocalDateTime.of(tomorrow, LocalTime.of(10, 0));
    }

    private CallTaskDto mapToDto(CallTask callTask) {
        return CallTaskDto.builder()
            .id(callTask.getId())
            .leadId(callTask.getLead().getId())
            .leadName(callTask.getLead().getName())
            .leadPhone(callTask.getLead().getPhone())
            .agentId(callTask.getAgent().getId())
            .agentName(callTask.getAgent().getName())
            .scheduledTime(callTask.getScheduledTime())
            .status(callTask.getStatus())
            .outcome(callTask.getOutcome())
            .notes(callTask.getNotes())
            .createdAt(callTask.getCreatedAt())
            .completedAt(callTask.getCompletedAt())
            .build();
    }
}
