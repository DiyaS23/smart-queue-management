package com.example.backend.controller;

import com.example.backend.dto.CounterRequest;
import com.example.backend.dto.TokenResponse;
import com.example.backend.entity.Counter;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenPriority;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.ServiceTypeRepository;
import com.example.backend.repository.TokenRepository;
import com.example.backend.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CounterRepository counterRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final TokenService tokenService;
    private final TokenRepository tokenRepository;

    @PostMapping("/services")
    public ServiceType createService(@RequestBody ServiceType serviceType) {
        return serviceTypeRepository.save(serviceType);
    }

    @PostMapping("/counters")
    public Counter createCounter(@RequestBody CounterRequest request) {
        Counter counter = new Counter();
        counter.setName(request.getName());
        counter.setStatus(request.getStatus());
        return counterRepository.save(counter);
    }

    @GetMapping("/counters")
    public List<Counter> getCounters() {
        return counterRepository.findAll();
    }

    @GetMapping("/emergencies")
    public List<TokenResponse> getPendingEmergencies() {
        return tokenRepository
                .findByStatusAndPriorityTypeAndApproved(
                        TokenStatus.PENDING_APPROVAL,
                        TokenPriority.URGENT,
                        false
                )
                .stream()
                .map(this::map)
                .toList();
    }

    @PutMapping("/emergencies/{tokenId}/approve")
    public TokenResponse approveEmergency(@PathVariable Long tokenId) {
        return map(tokenService.approveEmergency(tokenId));
    }

    @PutMapping("/emergencies/{tokenId}/reject")
    public void rejectEmergency(@PathVariable Long tokenId) {
        tokenService.rejectEmergency(tokenId);
    }
    private TokenResponse map(Token token) {
        TokenResponse res = new TokenResponse();
        res.setId(token.getId());
        res.setTokenNumber(token.getTokenNumber());
        res.setStatus(token.getStatus().name());
        res.setServiceName(token.getServiceType().getName());
        res.setPatientName(token.getPatient().getName());
        res.setDoctorName(token.getDoctor() != null ? token.getDoctor().getName() : null);
        res.setPriority(token.isPriority());
        res.setCreatedAt(token.getCreatedAt());
        return res;
    }

}
