package com.example.backend.service;

import com.example.backend.entity.ServiceMetric;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.CounterStatus;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.ServiceMetricRepository;
import com.example.backend.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EtaService {

    private final ServiceMetricRepository metricRepository;
    private final CounterRepository counterRepository;
    private final TokenRepository tokenRepository;

    public long calculateEtaMinutes(ServiceType serviceType, Token token) {
        if (token.getStatus() == TokenStatus.COMPLETED ||
                token.getStatus() == TokenStatus.SERVING) {
            return 0;
        }
        ServiceMetric metric = metricRepository
                .findByServiceType(serviceType)
                .orElseThrow(() -> new RuntimeException("Metrics not found"));

        long tokensAhead =
                tokenRepository.countByServiceTypeAndStatusAndCreatedAtBefore(
                        serviceType,
                        TokenStatus.WAITING,
                        token.getCreatedAt()
                );
        long activeCounters =
                counterRepository.findByStatus(CounterStatus.OPEN).size();

        if (activeCounters == 0) return -1; // infinite / paused

        double avgTime = metric.getAvgServiceTimeMinutes();

        return Math.round((tokensAhead * avgTime) / activeCounters);
    }
}

