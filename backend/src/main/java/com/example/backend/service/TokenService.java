package com.example.backend.service;

import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.ServiceTypeRepository;
import com.example.backend.repository.TokenRepository;
import com.example.backend.websocket.QueueEvent;
import com.example.backend.websocket.QueueEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final QueueEventPublisher eventPublisher;

    @Transactional
    public Token createToken(Long serviceTypeId, boolean priority) {
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        Token token = new Token();
        token.setServiceType(serviceType);
        token.setPriority(priority);
        token.setStatus(TokenStatus.WAITING);
        token.setCreatedAt(LocalDateTime.now());
        token.setTokenNumber(generateTokenNumber(serviceType));

        Token saved = tokenRepository.save(token);

        eventPublisher.publishQueueUpdate(
                new QueueEvent(
                        "TOKEN_CREATED",
                        saved.getTokenNumber(),
                        null,
                        serviceType.getName(),
                        saved.getStatus().name()
                )
        );

        return saved;
    }

    private String generateTokenNumber(ServiceType serviceType) {
        // Example: CASH → C101, DOCTOR → D205
        String prefix = serviceType.getName().substring(0, 1).toUpperCase();
        long count = tokenRepository.count();
        return prefix + (100 + count);
    }

    @Transactional
    public void updateStatus(Token token, TokenStatus status) {
        token.setStatus(status);
        if (status == TokenStatus.CALLED) {
            token.setCalledAt(LocalDateTime.now());
        }
        if (status == TokenStatus.COMPLETED) {
            token.setCompletedAt(LocalDateTime.now());
        }
        tokenRepository.save(token);
    }
}

