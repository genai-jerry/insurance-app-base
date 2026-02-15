package com.insurance.voice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NeedsExtractionService {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * Extract structured needs from a conversation transcript using AI
     */
    public Map<String, Object> extractNeeds(String transcript) {
        log.info("Extracting needs from transcript of length: {}", transcript.length());

        String extractionPrompt = buildExtractionPrompt(transcript);

        try {
            ChatClient chatClient = chatClientBuilder.build();

            String response = chatClient.prompt()
                .user(extractionPrompt)
                .call()
                .content();

            return parseNeedsResponse(response);

        } catch (Exception e) {
            log.error("Error extracting needs from transcript", e);
            return createFallbackNeeds();
        }
    }

    private String buildExtractionPrompt(String transcript) {
        return """
            You are an AI assistant helping to extract insurance needs from a customer conversation.

            Analyze the following conversation transcript and extract:
            1. Insurance types needed (life, health, auto, home, etc.)
            2. Coverage amount preferences
            3. Budget constraints
            4. Timeline for purchase
            5. Key concerns or priorities
            6. Family situation (dependents, spouse, etc.)
            7. Any existing coverage mentioned

            Transcript:
            %s

            Please provide the extracted information in a structured format with the following keys:
            - insuranceTypes: List of insurance types needed
            - coverageAmount: Preferred coverage amount
            - budget: Budget constraints
            - timeline: When they want to purchase
            - concerns: List of key concerns
            - familySituation: Family details
            - existingCoverage: Existing policies mentioned
            - additionalNotes: Any other relevant information

            Return the response as a JSON-like structure.
            """.formatted(transcript);
    }

    private Map<String, Object> parseNeedsResponse(String response) {
        Map<String, Object> needs = new HashMap<>();

        // Simple parsing - in production, you'd use proper JSON parsing
        // For now, create a structured response
        needs.put("rawExtraction", response);
        needs.put("insuranceTypes", extractListField(response, "insuranceTypes"));
        needs.put("coverageAmount", extractField(response, "coverageAmount"));
        needs.put("budget", extractField(response, "budget"));
        needs.put("timeline", extractField(response, "timeline"));
        needs.put("concerns", extractListField(response, "concerns"));
        needs.put("familySituation", extractField(response, "familySituation"));
        needs.put("existingCoverage", extractField(response, "existingCoverage"));
        needs.put("additionalNotes", extractField(response, "additionalNotes"));

        return needs;
    }

    private String extractField(String response, String fieldName) {
        // Simple extraction - in production, use proper JSON parsing
        String marker = fieldName + ":";
        int startIdx = response.indexOf(marker);
        if (startIdx == -1) {
            return "";
        }

        startIdx += marker.length();
        int endIdx = response.indexOf("\n", startIdx);
        if (endIdx == -1) {
            endIdx = response.length();
        }

        return response.substring(startIdx, endIdx).trim();
    }

    private List<String> extractListField(String response, String fieldName) {
        String content = extractField(response, fieldName);
        if (content.isEmpty()) {
            return List.of();
        }

        // Simple list parsing
        return List.of(content.split(",")).stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private Map<String, Object> createFallbackNeeds() {
        Map<String, Object> needs = new HashMap<>();
        needs.put("error", "Failed to extract needs");
        needs.put("insuranceTypes", List.of());
        needs.put("coverageAmount", "");
        needs.put("budget", "");
        needs.put("timeline", "");
        needs.put("concerns", List.of());
        needs.put("familySituation", "");
        needs.put("existingCoverage", "");
        needs.put("additionalNotes", "");
        return needs;
    }
}
