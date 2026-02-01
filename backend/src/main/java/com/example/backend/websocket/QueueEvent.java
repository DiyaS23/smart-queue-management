package com.example.backend.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QueueEvent {

    private String type;      // TOKEN_CREATED, TOKEN_CALLED, TOKEN_COMPLETED
    private String tokenNumber;
    private String counterName;
    private String serviceName;
    private String status;
}

