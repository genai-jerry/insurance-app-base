package com.insurance.scheduler.controller;

import com.insurance.scheduler.dto.CallTaskDto;
import com.insurance.scheduler.dto.CreateCallTaskRequest;
import com.insurance.scheduler.dto.UpdateCallTaskRequest;
import com.insurance.scheduler.service.CallTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/scheduler/tasks")
@RequiredArgsConstructor
@Tag(name = "Call Task Scheduler", description = "Endpoints for managing call tasks and scheduling")
public class CallTaskController {

    private final CallTaskService callTaskService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Create a new call task")
    public ResponseEntity<CallTaskDto> createCallTask(@Valid @RequestBody CreateCallTaskRequest request) {
        CallTaskDto created = callTaskService.createCallTask(request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get today's call tasks")
    public ResponseEntity<List<CallTaskDto>> getTodayCallTasks() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        List<CallTaskDto> tasks = callTaskService.getCallTasksByDateRange(startOfDay, endOfDay);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get call task by ID")
    public ResponseEntity<CallTaskDto> getCallTask(@PathVariable Long id) {
        CallTaskDto callTask = callTaskService.getCallTask(id);
        return ResponseEntity.ok(callTask);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Update call task")
    public ResponseEntity<CallTaskDto> updateCallTask(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCallTaskRequest request
    ) {
        CallTaskDto updated = callTaskService.updateCallTask(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Delete call task")
    public ResponseEntity<Void> deleteCallTask(@PathVariable Long id) {
        callTaskService.deleteCallTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get all call tasks for an agent")
    public ResponseEntity<List<CallTaskDto>> getCallTasksByAgent(@PathVariable Long agentId) {
        List<CallTaskDto> tasks = callTaskService.getCallTasksByAgent(agentId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/lead/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get all call tasks for a lead")
    public ResponseEntity<List<CallTaskDto>> getCallTasksByLead(@PathVariable Long leadId) {
        List<CallTaskDto> tasks = callTaskService.getCallTasksByLead(leadId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get call tasks by date range")
    public ResponseEntity<List<CallTaskDto>> getCallTasksByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        List<CallTaskDto> tasks = callTaskService.getCallTasksByDateRange(startTime, endTime);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/agent/{agentId}/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get call tasks for an agent by date range")
    public ResponseEntity<List<CallTaskDto>> getCallTasksByAgentAndDateRange(
        @PathVariable Long agentId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        List<CallTaskDto> tasks = callTaskService.getCallTasksByAgentAndDateRange(agentId, startTime, endTime);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get all pending call tasks")
    public ResponseEntity<List<CallTaskDto>> getPendingCallTasks() {
        List<CallTaskDto> tasks = callTaskService.getPendingCallTasks();
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Mark call task as complete")
    public ResponseEntity<CallTaskDto> markComplete(
        @PathVariable Long id,
        @RequestParam(required = false) String outcome,
        @RequestParam(required = false) String notes
    ) {
        CallTaskDto updated = callTaskService.markComplete(id, outcome, notes);
        return ResponseEntity.ok(updated);
    }
}
