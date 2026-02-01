package com.example.backend.service;

import com.example.backend.dto.AdminDashboardSummary;
import com.example.backend.dto.DoctorLoadResponse;
import com.example.backend.dto.ServiceStatsResponse;
import com.example.backend.entity.ServiceMetric;
import com.example.backend.entity.enums.TokenPriority;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.ServiceMetricRepository;
import com.example.backend.repository.ServiceTypeRepository;
import com.example.backend.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final TokenRepository tokenRepository;
    private final CounterRepository counterRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceMetricRepository metricRepository;

    public AdminDashboardSummary getSummary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        return new AdminDashboardSummary(
                tokenRepository.countByStatusAndCompletedAtBetween(
                        TokenStatus.COMPLETED, startOfDay, now),
                tokenRepository.countByStatus(TokenStatus.WAITING),
                tokenRepository.countByStatusAndPriorityType(
                        TokenStatus.PENDING_APPROVAL, TokenPriority.URGENT),
                tokenRepository.countByStatusAndPriorityType(
                        TokenStatus.WAITING, TokenPriority.URGENT)
        );
    }

    public List<DoctorLoadResponse> doctorLoad() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        return counterRepository.findAll().stream()
                .map(doc -> new DoctorLoadResponse(
                        doc.getId(),
                        doc.getName(),
                        tokenRepository.countByDoctorAndStatus(doc, TokenStatus.WAITING),
                        tokenRepository.countByDoctorAndStatus(doc, TokenStatus.SERVING),
                        tokenRepository.countByDoctorAndStatusAndCompletedAtBetween(
                                doc, TokenStatus.COMPLETED, startOfDay, now)
                ))
                .toList();
    }

    public List<ServiceStatsResponse> serviceStats() {
        return serviceTypeRepository.findAll().stream()
                .map(service -> {
                    double avg = metricRepository
                            .findByServiceType(service)
                            .map(ServiceMetric::getAvgServiceTimeMinutes)
                            .orElse(0.0);

                    long waiting = tokenRepository
                            .countByServiceTypeAndStatus(service, TokenStatus.WAITING);

                    return new ServiceStatsResponse(
                            service.getId(),
                            service.getName(),
                            avg,
                            waiting
                    );
                })
                .toList();
    }
}

