package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponse {
    private Long id;
    private String tokenNumber;
    private String serviceName;
    private String status;
    private boolean priority;
    private String counterName;
    private LocalDateTime createdAt;
}
