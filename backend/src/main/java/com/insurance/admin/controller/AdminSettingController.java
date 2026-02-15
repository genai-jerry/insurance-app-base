package com.insurance.admin.controller;

import com.insurance.admin.dto.AdminSettingDto;
import com.insurance.admin.dto.UpdateSettingRequest;
import com.insurance.admin.service.AdminSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@Tag(name = "Admin Settings", description = "Endpoints for managing system settings (admin only)")
public class AdminSettingController {

    private final AdminSettingService adminSettingService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all settings")
    public ResponseEntity<List<AdminSettingDto>> getAllSettings() {
        List<AdminSettingDto> settings = adminSettingService.getAllSettings();
        return ResponseEntity.ok(settings);
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get setting by key")
    public ResponseEntity<AdminSettingDto> getSetting(@PathVariable String key) {
        AdminSettingDto setting = adminSettingService.getSetting(key);
        return ResponseEntity.ok(setting);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new setting")
    public ResponseEntity<AdminSettingDto> createSetting(
        @Valid @RequestBody UpdateSettingRequest request,
        Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        AdminSettingDto created = adminSettingService.createSetting(request, userId);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing setting")
    public ResponseEntity<AdminSettingDto> updateSetting(
        @PathVariable String key,
        @Valid @RequestBody UpdateSettingRequest request,
        Authentication authentication
    ) {
        request.setKey(key); // Ensure key matches path parameter
        Long userId = extractUserId(authentication);
        AdminSettingDto updated = adminSettingService.updateSetting(request, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a setting")
    public ResponseEntity<Void> deleteSetting(
        @PathVariable String key,
        Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        adminSettingService.deleteSetting(key, userId);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Authentication authentication) {
        // Extract user ID from authentication
        // This assumes UserDetails has getId() method or similar
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            // In real implementation, extract from custom UserDetails
            return 1L; // Placeholder
        }
        return null;
    }
}
