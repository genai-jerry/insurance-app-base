package com.insurance.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSessionResponse {

    private Long sessionId;
    private String openaiSessionId;
    private String status;
    private String message;
    private String websocketUrl;
}
