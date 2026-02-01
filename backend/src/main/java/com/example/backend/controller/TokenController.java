package com.example.backend.controller;

import com.example.backend.dto.CreatePatientTokenRequest;
import com.example.backend.dto.CreateTokenRequest;
import com.example.backend.dto.QueueStatusResponse;
import com.example.backend.dto.TokenResponse;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.service.QueueService;
import com.example.backend.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

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
    @PostMapping("/patient")
    public TokenResponse createPatientToken(
            @RequestBody @Valid CreatePatientTokenRequest request) {

        Token token = tokenService.createPatientToken(request);
        return mapToResponse(token);
    }

    @GetMapping("/status/{status}")
    public List<TokenResponse> getTokensByStatus(@PathVariable String status) {
        // Convert String to Enum safely
        TokenStatus tokenStatus = TokenStatus.valueOf(status.toUpperCase());

        // You need to ensure tokenService has this method (see Step 2)
        List<Token> tokens = tokenService.getTokensByStatus(tokenStatus);

        return tokens.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    @GetMapping("/queue/{serviceTypeId}")
    public QueueStatusResponse getQueueStatus(@PathVariable Long serviceTypeId) {
        QueueStatusResponse response = new QueueStatusResponse();
        response.setWaitingCount(queueService.getWaitingCount(serviceTypeId));
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
                token.getDoctor() != null ? token.getDoctor().getName() : null
        );
        res.setCreatedAt(token.getCreatedAt());
        res.setPatientName(
                token.getPatient() != null ? token.getPatient().getName() : null
        );
        res.setDoctorName(
                token.getDoctor() != null ? token.getDoctor().getName() : null
        );
        return res;
    }
}
