package com.insurance.admin.service;

import com.insurance.auth.dto.UserDto;
import com.insurance.auth.repository.UserRepository;
import com.insurance.common.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToDto(user);
    }

    @Transactional
    public UserDto createUser(String name, String email, String password, User.Role role, Long adminId) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User already exists with email: " + email);
        }

        User user = User.builder()
            .name(name)
            .email(email)
            .hashedPassword(passwordEncoder.encode(password))
            .role(role)
            .build();

        User saved = userRepository.save(user);

        // Log the creation
        auditLogService.logAction(
            adminId,
            "CREATE_USER",
            "User",
            saved.getId(),
            null,
            saved.getEmail(),
            null
        );

        log.info("Created user {} with role {} by admin {}", email, role, adminId);

        return mapToDto(saved);
    }

    @Transactional
    public UserDto updateUser(Long id, String name, String email, User.Role role, Long adminId) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        String oldEmail = user.getEmail();

        if (name != null) {
            user.setName(name);
        }

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new RuntimeException("User already exists with email: " + email);
            }
            user.setEmail(email);
        }

        if (role != null) {
            user.setRole(role);
        }

        User updated = userRepository.save(user);

        // Log the update
        auditLogService.logAction(
            adminId,
            "UPDATE_USER",
            "User",
            updated.getId(),
            oldEmail,
            updated.getEmail(),
            null
        );

        log.info("Updated user {} by admin {}", id, adminId);

        return mapToDto(updated);
    }

    @Transactional
    public void deleteUser(Long id, Long adminId) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Log the deletion
        auditLogService.logAction(
            adminId,
            "DELETE_USER",
            "User",
            user.getId(),
            user.getEmail(),
            null,
            null
        );

        userRepository.delete(user);
        log.info("Deleted user {} by admin {}", id, adminId);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword, Long adminId) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Log the password reset
        auditLogService.logAction(
            adminId,
            "RESET_PASSWORD",
            "User",
            user.getId(),
            null,
            "Password reset",
            null
        );

        log.info("Reset password for user {} by admin {}", id, adminId);
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
