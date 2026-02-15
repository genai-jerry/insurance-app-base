package com.insurance.voice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.insurance.common.entity.VoiceSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSessionDto {

    private Long id;
    private Long leadId;
    private String leadName;
    private Long agentId;
    private String agentName;
    private Long callTaskId;
    private String sessionId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endedAt;

    private Integer durationSeconds;
    private String transcriptText;
    private Map<String, Object> extractedNeedsJson;
    private Map<String, Object> recommendationsJson;
    private VoiceSession.SessionStatus status;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
