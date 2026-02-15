package com.insurance.voice.controller;

import com.insurance.voice.dto.StartVoiceSessionRequest;
import com.insurance.voice.dto.VoiceSessionDto;
import com.insurance.voice.dto.VoiceSessionResponse;
import com.insurance.voice.service.VoiceBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice/sessions")
@RequiredArgsConstructor
@Tag(name = "Voice Sessions", description = "Endpoints for managing voice bot sessions")
public class VoiceSessionController {

    private final VoiceBotService voiceBotService;

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Start a new voice session")
    public ResponseEntity<VoiceSessionResponse> startSession(@Valid @RequestBody StartVoiceSessionRequest request) {
        VoiceSessionResponse response = voiceBotService.startVoiceSession(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Stop a voice session")
    public ResponseEntity<VoiceSessionDto> stopSession(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String transcript = body != null ? body.get("transcript") : "";
        VoiceSessionDto dto = voiceBotService.stopVoiceSession(id, transcript);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get voice session by ID")
    public ResponseEntity<VoiceSessionDto> getSession(@PathVariable Long id) {
        VoiceSessionDto dto = voiceBotService.getVoiceSession(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/lead/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get all voice sessions for a lead")
    public ResponseEntity<List<VoiceSessionDto>> getSessionsByLead(@PathVariable Long leadId) {
        List<VoiceSessionDto> sessions = voiceBotService.getVoiceSessionsByLead(leadId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get all voice sessions for an agent")
    public ResponseEntity<List<VoiceSessionDto>> getSessionsByAgent(@PathVariable Long agentId) {
        List<VoiceSessionDto> sessions = voiceBotService.getVoiceSessionsByAgent(agentId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}/transcript")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get transcript for a voice session")
    public ResponseEntity<Map<String, String>> getTranscript(@PathVariable Long id) {
        String transcript = voiceBotService.getTranscript(id);
        return ResponseEntity.ok(Map.of("transcript", transcript != null ? transcript : ""));
    }

    @PostMapping("/{id}/extract-needs")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Extract needs from voice session transcript")
    public ResponseEntity<VoiceSessionDto> extractNeeds(@PathVariable Long id) {
        VoiceSessionDto dto = voiceBotService.extractNeeds(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/transcript")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Update transcript for a voice session")
    public ResponseEntity<VoiceSessionDto> updateTranscript(
        @PathVariable Long id,
        @RequestBody Map<String, String> body
    ) {
        String transcript = body.get("transcript");
        VoiceSessionDto dto = voiceBotService.updateTranscript(id, transcript);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/needs")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get extracted needs from voice session")
    public ResponseEntity<Map<String, Object>> getNeeds(@PathVariable Long id) {
        VoiceSessionDto session = voiceBotService.getVoiceSession(id);
        Map<String, Object> needs = session.getExtractedNeedsJson();
        return ResponseEntity.ok(needs != null ? needs : Map.of());
    }

    @GetMapping("/{id}/recommendations")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get product recommendations from voice session")
    public ResponseEntity<Map<String, Object>> getRecommendations(@PathVariable Long id) {
        VoiceSessionDto session = voiceBotService.getVoiceSession(id);
        Map<String, Object> recommendations = session.getRecommendationsJson();
        return ResponseEntity.ok(recommendations != null ? recommendations : Map.of());
    }
}
