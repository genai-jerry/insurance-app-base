package com.insurance.admin.controller;

import com.insurance.admin.service.UserManagementService;
import com.insurance.auth.dto.UserDto;
import com.insurance.common.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing users (admin only)")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userManagementService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        UserDto user = userManagementService.getUser(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserDto> createUser(
        @Valid @RequestBody CreateUserRequest request,
        Authentication authentication
    ) {
        Long adminId = extractUserId(authentication);
        UserDto created = userManagementService.createUser(
            request.getName(),
            request.getEmail(),
            request.getPassword(),
            User.Role.valueOf(request.getRole()),
            adminId
        );
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user")
    public ResponseEntity<UserDto> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest request,
        Authentication authentication
    ) {
        Long adminId = extractUserId(authentication);
        UserDto updated = userManagementService.updateUser(
            id,
            request.getName(),
            request.getEmail(),
            request.getRole() != null ? User.Role.valueOf(request.getRole()) : null,
            adminId
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user")
    public ResponseEntity<Void> deleteUser(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Long adminId = extractUserId(authentication);
        userManagementService.deleteUser(id, adminId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset user password")
    public ResponseEntity<Void> resetPassword(
        @PathVariable Long id,
        @Valid @RequestBody ResetPasswordRequest request,
        Authentication authentication
    ) {
        Long adminId = extractUserId(authentication);
        userManagementService.resetPassword(id, request.getNewPassword(), adminId);
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserRequest {
        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        @NotBlank(message = "Role is required")
        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserRequest {
        private String name;

        @Email(message = "Invalid email format")
        private String email;

        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        @NotBlank(message = "New password is required")
        private String newPassword;
    }
}
