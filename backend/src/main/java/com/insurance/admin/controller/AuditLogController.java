package com.insurance.admin.controller;

import com.insurance.admin.dto.AuditLogDto;
import com.insurance.admin.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Endpoints for viewing audit logs (admin only)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all audit logs with pagination")
    public ResponseEntity<Page<AuditLogDto>> getAllAuditLogs(
        @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditLogDto> auditLogs = auditLogService.getAllAuditLogs(pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/actor/{actorId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs for a specific user")
    public ResponseEntity<List<AuditLogDto>> getAuditLogsByActor(@PathVariable Long actorId) {
        List<AuditLogDto> auditLogs = auditLogService.getAuditLogsByActor(actorId);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/actor/{actorId}/page")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs for a specific user with pagination")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogsByActorPaged(
        @PathVariable Long actorId,
        @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogsByActor(actorId, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/entity/{entity}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs for a specific entity type")
    public ResponseEntity<List<AuditLogDto>> getAuditLogsByEntity(@PathVariable String entity) {
        List<AuditLogDto> auditLogs = auditLogService.getAuditLogsByEntity(entity);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/entity/{entity}/page")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs for a specific entity type with pagination")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogsByEntityPaged(
        @PathVariable String entity,
        @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogsByEntity(entity, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/entity/{entity}/{entityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs for a specific entity instance")
    public ResponseEntity<List<AuditLogDto>> getAuditLogsByEntityAndId(
        @PathVariable String entity,
        @PathVariable Long entityId
    ) {
        List<AuditLogDto> auditLogs = auditLogService.getAuditLogsByEntityAndId(entity, entityId);
        return ResponseEntity.ok(auditLogs);
    }
}
