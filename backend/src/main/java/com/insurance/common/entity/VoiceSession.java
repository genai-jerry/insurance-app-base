package com.insurance.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "voice_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_task_id")
    private CallTask callTask;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "transcript_text", columnDefinition = "TEXT")
    private String transcriptText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_needs_json", columnDefinition = "jsonb")
    private Map<String, Object> extractedNeedsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations_json", columnDefinition = "jsonb")
    private Map<String, Object> recommendationsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum SessionStatus {
        IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }
}
