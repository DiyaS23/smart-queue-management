package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PatientTokenHistoryResponse {
    private Long tokenId;
    private String tokenNumber;
    private String serviceName;
    private String doctorName; // nullable
    private String status;
    private boolean urgent;
    private LocalDateTime createdAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;
}

