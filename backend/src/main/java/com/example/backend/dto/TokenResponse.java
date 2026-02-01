package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TokenResponse {

    private Long id;
    private String tokenNumber;
    private String serviceName;
    private String status;
    private boolean priority;
    private LocalDateTime createdAt;
    private Long tokenId;
    private String doctorName;
}
