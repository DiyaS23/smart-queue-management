package com.example.backend.controller;

import com.example.backend.entity.Token;
import com.example.backend.repository.ServiceTypeRepository;
import com.example.backend.repository.TokenRepository;
import com.example.backend.service.EtaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final EtaService etaService;
    private final ServiceTypeRepository serviceTypeRepository;
    private final TokenRepository tokenRepository;

    @GetMapping("/eta/{tokenId}")
    public long getEta(@PathVariable Long tokenId) {

        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        return etaService.calculateEtaMinutes(
                token.getServiceType(),
                token
        );
    }
}

