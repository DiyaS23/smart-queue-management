package com.example.backend.controller;

import com.example.backend.dto.CreateTokenRequest;
import com.example.backend.dto.QueueStatusResponse;
import com.example.backend.dto.TokenResponse;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.service.QueueService;
import com.example.backend.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import lombok.Data;
@Data
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenController {
    private final TokenService tokenService;
    private final QueueService queueService;

    @PostMapping
    public TokenResponse createToken(@RequestBody CreateTokenRequest request) {
        Token token = tokenService.createToken(
                request.getServiceTypeId(),
                request.isPriority()
        );
        return mapToResponse(token);
    }

    @GetMapping("/queue/{serviceTypeId}")
    public QueueStatusResponse getQueueStatus(@PathVariable Long serviceTypeId) {
        QueueStatusResponse response = new QueueStatusResponse();
        response.setWaitingCount(queueService.getWaitingCount(
                new ServiceType(serviceTypeId)
        ));
        return response;
    }

    private TokenResponse mapToResponse(Token token) {
        TokenResponse res = new TokenResponse();
        res.setId(token.getId());
        res.setTokenNumber(token.getTokenNumber());
        res.setServiceName(token.getServiceType().getName());
        res.setStatus(token.getStatus().name());
        res.setPriority(token.isPriority());
        res.setCounterName(
                token.getCounter() != null ? token.getCounter().getName() : null
        );
        res.setCreatedAt(token.getCreatedAt());
        return res;
    }
}
