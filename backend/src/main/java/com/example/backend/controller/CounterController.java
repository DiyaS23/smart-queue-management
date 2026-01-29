package com.example.backend.controller;

import com.example.backend.dto.TokenResponse;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.repository.ServiceTypeRepository;
import com.example.backend.service.CounterService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

@RestController
@RequestMapping("/api/counters")
@RequiredArgsConstructor
public class CounterController {
    private final CounterService counterService;
    private final ServiceTypeRepository serviceTypeRepository;

    @PostMapping("/{counterId}/call-next/{serviceTypeId}")
    public TokenResponse callNext(
            @PathVariable Long counterId,
            @PathVariable Long serviceTypeId
    ) {
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        Token token = counterService.callNextToken(counterId, serviceType);
        return map(token);
    }

    @PutMapping("/tokens/{tokenId}/complete")
    public void completeToken(@PathVariable Long tokenId) {
        counterService.completeToken(tokenId);
    }

    @PutMapping("/tokens/{tokenId}/skip")
    public void skipToken(@PathVariable Long tokenId) {
        counterService.skipToken(tokenId);
    }

    private TokenResponse map(Token token) {
        TokenResponse res = new TokenResponse();
        res.setId(token.getId());
        res.setTokenNumber(token.getTokenNumber());
        res.setServiceName(token.getServiceType().getName());
        res.setStatus(token.getStatus().name());
        res.setCounterName(token.getCounter().getName());
        return res;
    }
}
