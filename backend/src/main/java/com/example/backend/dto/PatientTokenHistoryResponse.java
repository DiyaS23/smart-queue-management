package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PatientTokenHistoryResponse {

    private String tokenNumber;
    private String serviceName;
    private String doctorName; // nullable
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}

