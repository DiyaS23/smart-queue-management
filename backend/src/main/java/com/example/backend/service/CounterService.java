package com.example.backend.service;


import com.example.backend.entity.Counter;
import com.example.backend.entity.ServiceMetric;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.CounterStatus;
import com.example.backend.entity.enums.DoctorAvailability;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.ServiceMetricRepository;
import com.example.backend.repository.TokenRepository;
import com.example.backend.websocket.QueueEvent;
import com.example.backend.websocket.QueueEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounterService {



    private final CounterRepository counterRepository;
    private final TokenRepository tokenRepository;
    private final QueueService queueService;
    private final QueueEventPublisher eventPublisher;
    private final ServiceMetricRepository metricRepository;

//    @Transactional
//    public Token callNextToken(Long counterId, ServiceType serviceType) {
//
//        Counter counter = counterRepository.findById(counterId)
//                .orElseThrow(() -> new RuntimeException("Counter not found"));
//
//        if (counter.getStatus() != CounterStatus.OPEN) {
//            throw new RuntimeException("Counter is not open");
//        }
//
//        // Prevent double serving (doctor already serving someone)
//        tokenRepository.findFirstByDoctorAndStatusOrderByCreatedAtAsc(
//                counter, TokenStatus.SERVING
//        ).ifPresent(t -> {
//            throw new RuntimeException("Doctor already serving a token");
//        });
//
//        Token nextToken = queueService.getNextToken(serviceType, counter);
//
//        nextToken.setDoctor(counter);
//        nextToken.setStatus(TokenStatus.SERVING);
//        nextToken.setCalledAt(LocalDateTime.now());
//
//        Token saved = tokenRepository.save(nextToken);
//
//        QueueEvent event = new QueueEvent(
//                "TOKEN_CALLED",
//                saved.getTokenNumber(),
//                counter.getName(),
//                serviceType.getName(),
//                saved.getStatus().name()
//        );
//
//// existing admin/staff update
//        eventPublisher.publishQueueUpdate(event);
//
//// 🔔 NEW: patient notification
//        eventPublisher.publishToPatient(
//                saved.getTokenNumber(),
//                new QueueEvent(
//                        "TOKEN_CALLED_FOR_PATIENT",
//                        saved.getTokenNumber(),
//                        counter.getName(),
//                        serviceType.getName(),
//                        "SERVING"
//                )
//        );
//
//        return saved;
//    }
@Transactional
public Token callNextToken(Long counterId, ServiceType serviceType) {

    Counter counter = counterRepository.findById(counterId).orElse(null);

    // ❌ instead of throwing → return null-safe
    if (counter == null) {
        log.warn("Counter not found");
        return null;
    }

    if (counter.getStatus() != CounterStatus.OPEN) {
        log.warn("Counter closed");
        return null;
    }

    // ✅ If already serving → return same token
    Token current = tokenRepository
            .findFirstByDoctorAndStatusOrderByCreatedAtAsc(counter, TokenStatus.SERVING)
            .orElse(null);

    if (current != null) {
        return current; // no error
    }

    // ✅ Get next token safely
    Token nextToken = null;
    try {
        nextToken = queueService.getNextToken(serviceType, counter);
    } catch (Exception e) {
        log.info("No patients in queue");
        return null; // IMPORTANT
    }

    if (nextToken == null) {
        return null;
    }

    nextToken.setDoctor(counter);
    nextToken.setStatus(TokenStatus.SERVING);
    nextToken.setCalledAt(LocalDateTime.now());

    Token saved = tokenRepository.save(nextToken);

    counter.setAvailability(DoctorAvailability.BUSY);
    counterRepository.save(counter);

    QueueEvent event = new QueueEvent(
            "TOKEN_CALLED",
            saved.getTokenNumber(),
            counter.getName(),
            serviceType.getName(),
            saved.getStatus().name()
    );

    eventPublisher.publishQueueUpdate(event);
    eventPublisher.publishToPatient(saved.getTokenNumber(), event);

    return saved;
}

    @Transactional
    public void completeToken(Long tokenId) {

        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (token.getStatus() != TokenStatus.SERVING) {
            throw new RuntimeException("Only SERVING tokens can be completed");
        }

        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedAt(LocalDateTime.now());
        tokenRepository.save(token);
        Counter doctor = token.getDoctor();
        if (doctor != null) {
            doctor.setAvailability(DoctorAvailability.AVAILABLE);
            counterRepository.save(doctor);
        }
        updateMetrics(token);

        eventPublisher.publishQueueUpdate(
                new QueueEvent(
                        "TOKEN_COMPLETED",
                        token.getTokenNumber(),
                        token.getDoctor().getName(),
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
    @Transactional
    public void updateAvailability(Long counterId, DoctorAvailability availability) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Counter not found"));

        counter.setAvailability(availability);
        counterRepository.save(counter);

        // Notify all dashboards that a doctor's status changed
        eventPublisher.publishCounterUpdate(new QueueEvent(
                "DOCTOR_STATUS_CHANGED",
                null,
                counter.getName(),
                null,
                availability.name()
        ));
    }
    private void updateMetrics(Token token) {

        if (token.getCalledAt() == null || token.getCompletedAt() == null) {
            return;
        }

        ServiceMetric metric = metricRepository
                .findByServiceType(token.getServiceType())
                .orElseGet(() -> {
                    ServiceMetric m = new ServiceMetric();
                    m.setServiceType(token.getServiceType());
                    m.setAvgServiceTimeMinutes(5);
                    m.setTotalTokensServed(0);
                    return m;
                });

        long serviceTime =
                Duration.between(token.getCalledAt(), token.getCompletedAt())
                        .toMinutes();

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

