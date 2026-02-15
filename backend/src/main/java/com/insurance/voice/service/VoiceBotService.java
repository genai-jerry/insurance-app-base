package com.insurance.voice.service;

import com.insurance.common.entity.CallTask;
import com.insurance.common.entity.Lead;
import com.insurance.common.entity.User;
import com.insurance.common.entity.VoiceSession;
import com.insurance.leads.repository.LeadRepository;
import com.insurance.auth.repository.UserRepository;
import com.insurance.scheduler.repository.CallTaskRepository;
import com.insurance.voice.dto.StartVoiceSessionRequest;
import com.insurance.voice.dto.VoiceSessionDto;
import com.insurance.voice.dto.VoiceSessionResponse;
import com.insurance.voice.repository.VoiceSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceBotService {

    private final VoiceSessionRepository voiceSessionRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final CallTaskRepository callTaskRepository;
    private final NeedsExtractionService needsExtractionService;

    @Value("${app.voice.mock-mode:true}")
    private boolean mockModeDefault;

    @Value("${app.openai.realtime-model:gpt-4o-realtime-preview-2024-12-17}")
    private String realtimeModel;

    @Transactional
    public VoiceSessionResponse startVoiceSession(StartVoiceSessionRequest request) {
        Lead lead = leadRepository.findById(request.getLeadId())
            .orElseThrow(() -> new RuntimeException("Lead not found with id: " + request.getLeadId()));

        User agent = userRepository.findById(request.getAgentId())
            .orElseThrow(() -> new RuntimeException("Agent not found with id: " + request.getAgentId()));

        CallTask callTask = null;
        if (request.getCallTaskId() != null) {
            callTask = callTaskRepository.findById(request.getCallTaskId())
                .orElseThrow(() -> new RuntimeException("Call task not found with id: " + request.getCallTaskId()));
        }

        boolean mockMode = request.getMockMode() != null ? request.getMockMode() : mockModeDefault;

        String sessionId = UUID.randomUUID().toString();

        VoiceSession voiceSession = VoiceSession.builder()
            .lead(lead)
            .agent(agent)
            .callTask(callTask)
            .sessionId(sessionId)
            .startedAt(LocalDateTime.now())
            .status(VoiceSession.SessionStatus.IN_PROGRESS)
            .build();

        VoiceSession saved = voiceSessionRepository.save(voiceSession);
        log.info("Started voice session {} for lead {} with agent {}, mockMode: {}", sessionId, lead.getId(), agent.getId(), mockMode);

        if (mockMode) {
            return VoiceSessionResponse.builder()
                .sessionId(saved.getId())
                .openaiSessionId(sessionId)
                .status("IN_PROGRESS")
                .message("Mock mode: Voice session started")
                .build();
        }

        // In real mode, we would initialize OpenAI Realtime API WebSocket connection here
        // For now, return basic response
        return VoiceSessionResponse.builder()
            .sessionId(saved.getId())
            .openaiSessionId(sessionId)
            .status("IN_PROGRESS")
            .message("Voice session started")
            .websocketUrl("wss://api.openai.com/v1/realtime?model=" + realtimeModel)
            .build();
    }

    @Transactional
    public VoiceSessionDto stopVoiceSession(Long sessionId, String transcript) {
        VoiceSession voiceSession = voiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Voice session not found with id: " + sessionId));

        voiceSession.setEndedAt(LocalDateTime.now());
        voiceSession.setStatus(VoiceSession.SessionStatus.COMPLETED);
        voiceSession.setTranscriptText(transcript);

        if (voiceSession.getStartedAt() != null && voiceSession.getEndedAt() != null) {
            Duration duration = Duration.between(voiceSession.getStartedAt(), voiceSession.getEndedAt());
            voiceSession.setDurationSeconds((int) duration.getSeconds());
        }

        VoiceSession updated = voiceSessionRepository.save(voiceSession);
        log.info("Stopped voice session {}", sessionId);

        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public VoiceSessionDto getVoiceSession(Long id) {
        VoiceSession voiceSession = voiceSessionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Voice session not found with id: " + id));
        return mapToDto(voiceSession);
    }

    @Transactional(readOnly = true)
    public List<VoiceSessionDto> getVoiceSessionsByLead(Long leadId) {
        return voiceSessionRepository.findByLeadId(leadId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VoiceSessionDto> getVoiceSessionsByAgent(Long agentId) {
        return voiceSessionRepository.findByAgentId(agentId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String getTranscript(Long sessionId) {
        VoiceSession voiceSession = voiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Voice session not found with id: " + sessionId));
        return voiceSession.getTranscriptText();
    }

    @Transactional
    public VoiceSessionDto extractNeeds(Long sessionId) {
        VoiceSession voiceSession = voiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Voice session not found with id: " + sessionId));

        if (voiceSession.getTranscriptText() == null || voiceSession.getTranscriptText().isEmpty()) {
            throw new RuntimeException("No transcript available for session: " + sessionId);
        }

        var extractedNeeds = needsExtractionService.extractNeeds(voiceSession.getTranscriptText());
        voiceSession.setExtractedNeedsJson(extractedNeeds);

        VoiceSession updated = voiceSessionRepository.save(voiceSession);
        log.info("Extracted needs from voice session {}", sessionId);

        return mapToDto(updated);
    }

    @Transactional
    public VoiceSessionDto updateTranscript(Long sessionId, String transcript) {
        VoiceSession voiceSession = voiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Voice session not found with id: " + sessionId));

        voiceSession.setTranscriptText(transcript);
        VoiceSession updated = voiceSessionRepository.save(voiceSession);
        log.info("Updated transcript for voice session {}", sessionId);

        return mapToDto(updated);
    }

    private VoiceSessionDto mapToDto(VoiceSession voiceSession) {
        return VoiceSessionDto.builder()
            .id(voiceSession.getId())
            .leadId(voiceSession.getLead().getId())
            .leadName(voiceSession.getLead().getName())
            .agentId(voiceSession.getAgent().getId())
            .agentName(voiceSession.getAgent().getName())
            .callTaskId(voiceSession.getCallTask() != null ? voiceSession.getCallTask().getId() : null)
            .sessionId(voiceSession.getSessionId())
            .startedAt(voiceSession.getStartedAt())
            .endedAt(voiceSession.getEndedAt())
            .durationSeconds(voiceSession.getDurationSeconds())
            .transcriptText(voiceSession.getTranscriptText())
            .extractedNeedsJson(voiceSession.getExtractedNeedsJson())
            .recommendationsJson(voiceSession.getRecommendationsJson())
            .status(voiceSession.getStatus())
            .errorMessage(voiceSession.getErrorMessage())
            .createdAt(voiceSession.getCreatedAt())
            .build();
    }
}
