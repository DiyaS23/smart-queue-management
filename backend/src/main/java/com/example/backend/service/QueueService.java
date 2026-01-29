package com.example.backend.service;

import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final TokenRepository tokenRepository;

    /**
     * Core queue algorithm:
     * 1. Priority tokens first
     * 2. FIFO within same priority
     */
    @Transactional
    public Token getNextToken(ServiceType serviceType) {

        // 1️⃣ Priority tokens
        List<Token> priorityTokens =
                tokenRepository.findByServiceTypeAndStatusOrderByCreatedAtAsc(
                                serviceType, TokenStatus.WAITING
                        ).stream()
                        .filter(Token::isPriority)
                        .toList();

        if (!priorityTokens.isEmpty()) {
            return priorityTokens.get(0);
        }

        // 2️⃣ Normal FIFO
        List<Token> normalTokens =
                tokenRepository.findByServiceTypeAndStatusOrderByCreatedAtAsc(
                                serviceType, TokenStatus.WAITING
                        ).stream()
                        .filter(t -> !t.isPriority())
                        .toList();

        if (normalTokens.isEmpty()) {
            throw new RuntimeException("No tokens in queue");
        }

        return normalTokens.get(0);
    }

    public long getWaitingCount(ServiceType serviceType) {
        return tokenRepository.countByServiceTypeAndStatus(
                serviceType, TokenStatus.WAITING
        );
    }
}

