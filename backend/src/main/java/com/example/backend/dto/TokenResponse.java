package com.example.backend.dto;

import java.time.LocalDateTime;

public class TokenResponse {
    private Long id;
    private String tokenNumber;
    private String serviceName;
    private String status;
    private boolean priority;
    private String counterName;
    private LocalDateTime createdAt;
}
