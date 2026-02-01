package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ServiceStatsResponse {
    private Long serviceId;
    private String serviceName;
    private double avgServiceTimeMinutes;
    private long waitingCount;
}

