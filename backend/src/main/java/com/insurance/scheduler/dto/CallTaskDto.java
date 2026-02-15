package com.insurance.scheduler.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.insurance.common.entity.CallTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallTaskDto {

    private Long id;
    private Long leadId;
    private String leadName;
    private String leadPhone;
    private Long agentId;
    private String agentName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledTime;

    private CallTask.TaskStatus status;
    private String outcome;
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;
}
