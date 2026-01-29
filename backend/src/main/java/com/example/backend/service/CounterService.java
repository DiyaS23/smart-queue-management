package com.example.backend.service;

import com.example.backend.entity.Counter;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.CounterStatus;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CounterService {

    private final CounterRepository counterRepository;
    private final TokenRepository tokenRepository;
    private final QueueService queueService;

    @Transactional
    public Token callNextToken(Long counterId, ServiceType serviceType) {

        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Counter not found"));

        if (counter.getStatus() != CounterStatus.OPEN) {
            throw new RuntimeException("Counter is not open");
        }

        // Prevent double serving
        tokenRepository.findByCounterAndStatus(counter, TokenStatus.SERVING)
                .ifPresent(t -> {
                    throw new RuntimeException("Counter already serving a token");
                });

        Token nextToken = queueService.getNextToken(serviceType);

        nextToken.setCounter(counter);
        nextToken.setStatus(TokenStatus.SERVING);
        nextToken.setCalledAt(LocalDateTime.now());

        return tokenRepository.save(nextToken);
    }

    @Transactional
    public void completeToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedAt(LocalDateTime.now());

        tokenRepository.save(token);
    }

    @Transactional
    public void skipToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(TokenStatus.SKIPPED);
        tokenRepository.save(token);
    }
}

