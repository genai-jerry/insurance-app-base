package com.insurance.admin.service;

import com.insurance.admin.dto.AuditLogDto;
import com.insurance.admin.repository.AuditLogRepository;
import com.insurance.auth.repository.UserRepository;
import com.insurance.common.entity.AuditLog;
import com.insurance.common.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void logAction(
        Long actorId,
        String action,
        String entity,
        Long entityId,
        Object beforeState,
        Object afterState,
        String ipAddress
    ) {
        User actor = null;
        if (actorId != null) {
            actor = userRepository.findById(actorId).orElse(null);
        }

        Map<String, Object> beforeJson = null;
        Map<String, Object> afterJson = null;

        if (beforeState != null) {
            beforeJson = convertToMap(beforeState);
        }

        if (afterState != null) {
            afterJson = convertToMap(afterState);
        }

        AuditLog auditLog = AuditLog.builder()
            .actor(actor)
            .action(action)
            .entity(entity)
            .entityId(entityId)
            .beforeJson(beforeJson)
            .afterJson(afterJson)
            .ipAddress(ipAddress)
            .build();

        auditLogRepository.save(auditLog);
        log.debug("Logged audit action: {} on {} by user {}", action, entity, actorId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllOrderByCreatedAtDesc(pageable)
            .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAuditLogsByActor(Long actorId) {
        return auditLogRepository.findByActorId(actorId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByActor(Long actorId, Pageable pageable) {
        return auditLogRepository.findByActorId(actorId, pageable)
            .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAuditLogsByEntity(String entity) {
        return auditLogRepository.findByEntity(entity).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByEntity(String entity, Pageable pageable) {
        return auditLogRepository.findByEntity(entity, pageable)
            .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAuditLogsByEntityAndId(String entity, Long entityId) {
        return auditLogRepository.findByEntityAndEntityId(entity, entityId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    private Map<String, Object> convertToMap(Object object) {
        if (object instanceof Map) {
            return (Map<String, Object>) object;
        } else if (object instanceof String) {
            Map<String, Object> map = new HashMap<>();
            map.put("value", object);
            return map;
        } else {
            // For complex objects, you might want to use Jackson to convert
            Map<String, Object> map = new HashMap<>();
            map.put("data", object.toString());
            return map;
        }
    }

    private AuditLogDto mapToDto(AuditLog auditLog) {
        return AuditLogDto.builder()
            .id(auditLog.getId())
            .actorId(auditLog.getActor() != null ? auditLog.getActor().getId() : null)
            .actorName(auditLog.getActor() != null ? auditLog.getActor().getName() : "System")
            .action(auditLog.getAction())
            .entity(auditLog.getEntity())
            .entityId(auditLog.getEntityId())
            .beforeJson(auditLog.getBeforeJson())
            .afterJson(auditLog.getAfterJson())
            .ipAddress(auditLog.getIpAddress())
            .userAgent(auditLog.getUserAgent())
            .createdAt(auditLog.getCreatedAt())
            .build();
    }
}
