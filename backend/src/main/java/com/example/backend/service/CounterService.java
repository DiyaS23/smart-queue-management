package com.example.backend.service;

import com.example.backend.entity.Counter;
import com.example.backend.entity.ServiceMetric;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.CounterStatus;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.ServiceMetricRepository;
import com.example.backend.repository.TokenRepository;
import com.example.backend.websocket.QueueEvent;
import com.example.backend.websocket.QueueEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CounterService {

    private final CounterRepository counterRepository;
    private final TokenRepository tokenRepository;
    private final QueueService queueService;
    private final QueueEventPublisher eventPublisher;
    private final ServiceMetricRepository metricRepository;

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

        Token saved = tokenRepository.save(nextToken);

        eventPublisher.publishQueueUpdate(
                new QueueEvent(
                        "TOKEN_CALLED",
                        saved.getTokenNumber(),
                        counter.getName(),
                        serviceType.getName(),
                        saved.getStatus().name()
                )
        );

        return saved;
    }

    @Transactional
    public void completeToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedAt(LocalDateTime.now());

        tokenRepository.save(token);
        updateMetrics(token);
        eventPublisher.publishQueueUpdate(
                new QueueEvent(
                        "TOKEN_COMPLETED",
                        token.getTokenNumber(),
                        token.getCounter().getName(),
                        token.getServiceType().getName(),
                        token.getStatus().name()
                )
        );
    }

    @Transactional
    public void skipToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(TokenStatus.SKIPPED);
        tokenRepository.save(token);
    }
    private void updateMetrics(Token token) {

        ServiceMetric metric = metricRepository
                .findByServiceType(token.getServiceType())
                .orElseGet(() -> {
                    ServiceMetric m = new ServiceMetric();
                    m.setServiceType(token.getServiceType());
                    m.setAvgServiceTimeMinutes(5); // initial default
                    m.setTotalTokensServed(0);
                    return m;
                });

        long serviceTime = Duration.between(
                token.getCalledAt(),
                token.getCompletedAt()
        ).toMinutes();

        long total = metric.getTotalTokensServed();

        double newAvg =
                ((metric.getAvgServiceTimeMinutes() * total) + serviceTime)
                        / (total + 1);

        metric.setAvgServiceTimeMinutes(newAvg);
        metric.setTotalTokensServed(total + 1);
        metric.setLastUpdated(LocalDateTime.now());

        metricRepository.save(metric);
    }

}

