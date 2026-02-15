package com.insurance.admin.service;

import com.insurance.admin.dto.AdminSettingDto;
import com.insurance.admin.dto.UpdateSettingRequest;
import com.insurance.admin.repository.AdminSettingRepository;
import com.insurance.auth.repository.UserRepository;
import com.insurance.common.entity.AdminSetting;
import com.insurance.common.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSettingService {

    private final AdminSettingRepository adminSettingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AdminSettingDto> getAllSettings() {
        return adminSettingRepository.findAll().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminSettingDto getSetting(String key) {
        AdminSetting setting = adminSettingRepository.findByKey(key)
            .orElseThrow(() -> new RuntimeException("Setting not found with key: " + key));
        return mapToDto(setting);
    }

    @Transactional(readOnly = true)
    public String getSettingValue(String key) {
        Optional<AdminSetting> setting = adminSettingRepository.findByKey(key);
        if (setting.isEmpty()) {
            return null;
        }
        return decrypt(setting.get().getEncryptedValue());
    }

    @Transactional
    public AdminSettingDto updateSetting(UpdateSettingRequest request, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        AdminSetting setting = adminSettingRepository.findByKey(request.getKey())
            .orElse(AdminSetting.builder()
                .key(request.getKey())
                .build());

        String oldValue = setting.getEncryptedValue();
        String encryptedValue = encrypt(request.getValue());

        setting.setEncryptedValue(encryptedValue);
        setting.setDescription(request.getDescription());
        setting.setUpdatedBy(user);

        AdminSetting saved = adminSettingRepository.save(setting);

        // Log the change
        auditLogService.logAction(
            userId,
            "UPDATE_SETTING",
            "AdminSetting",
            saved.getId(),
            oldValue != null ? "***" : null,
            "***",
            null
        );

        log.info("Updated setting {} by user {}", request.getKey(), userId);

        return mapToDto(saved);
    }

    @Transactional
    public AdminSettingDto createSetting(UpdateSettingRequest request, Long userId) {
        if (adminSettingRepository.existsByKey(request.getKey())) {
            throw new RuntimeException("Setting already exists with key: " + request.getKey());
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        String encryptedValue = encrypt(request.getValue());

        AdminSetting setting = AdminSetting.builder()
            .key(request.getKey())
            .encryptedValue(encryptedValue)
            .description(request.getDescription())
            .updatedBy(user)
            .build();

        AdminSetting saved = adminSettingRepository.save(setting);

        // Log the creation
        auditLogService.logAction(
            userId,
            "CREATE_SETTING",
            "AdminSetting",
            saved.getId(),
            null,
            "***",
            null
        );

        log.info("Created setting {} by user {}", request.getKey(), userId);

        return mapToDto(saved);
    }

    @Transactional
    public void deleteSetting(String key, Long userId) {
        AdminSetting setting = adminSettingRepository.findByKey(key)
            .orElseThrow(() -> new RuntimeException("Setting not found with key: " + key));

        // Log the deletion
        auditLogService.logAction(
            userId,
            "DELETE_SETTING",
            "AdminSetting",
            setting.getId(),
            "***",
            null,
            null
        );

        adminSettingRepository.delete(setting);
        log.info("Deleted setting {} by user {}", key, userId);
    }

    /**
     * Simple encryption using Base64 encoding
     * In production, use proper encryption like AES
     */
    private String encrypt(String value) {
        if (value == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    /**
     * Simple decryption using Base64 decoding
     * In production, use proper decryption like AES
     */
    private String decrypt(String encryptedValue) {
        if (encryptedValue == null) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedValue);
            return new String(decodedBytes);
        } catch (Exception e) {
            log.error("Failed to decrypt value", e);
            return null;
        }
    }

    private AdminSettingDto mapToDto(AdminSetting setting) {
        return AdminSettingDto.builder()
            .id(setting.getId())
            .key(setting.getKey())
            .value("***") // Never expose actual value in DTO
            .description(setting.getDescription())
            .updatedByUserId(setting.getUpdatedBy() != null ? setting.getUpdatedBy().getId() : null)
            .updatedByUserName(setting.getUpdatedBy() != null ? setting.getUpdatedBy().getName() : null)
            .updatedAt(setting.getUpdatedAt())
            .createdAt(setting.getCreatedAt())
            .build();
    }
}
