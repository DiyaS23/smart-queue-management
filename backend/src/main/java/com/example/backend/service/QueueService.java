package com.example.backend.service;

import com.example.backend.entity.Counter;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.DoctorAvailability;
import com.example.backend.entity.enums.TokenPriority;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.ServiceTypeRepository;
import com.example.backend.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final TokenRepository tokenRepository;
    private final CounterRepository counterRepository;
    private final ServiceTypeRepository serviceTypeRepository;

    /**
     * CORE QUEUE LOGIC (Phase‑4 safe)
     *
     * Priority order:
     * 1. Doctor‑specific waiting token (if doctor is assigned)
     * 2. Department‑level waiting token
     */
    @Transactional
    public Token getNextToken(ServiceType serviceType, Counter doctor) {

        Optional<Token> emergency =
                tokenRepository.findFirstByStatusAndApprovedAndPriorityTypeOrderByCreatedAtAsc(
                        TokenStatus.WAITING,
                        true,
                        TokenPriority.URGENT
                );

        if (emergency.isPresent()) {
            return emergency.get();
        }

        // 1️⃣ Doctor‑specific queue
        if (doctor != null) {
            Optional<Token> doctorToken =
                    tokenRepository.findFirstByDoctorAndStatusOrderByCreatedAtAsc(
                            doctor,
                            TokenStatus.WAITING
                    );

            if (doctorToken.isPresent()) {
                return doctorToken.get();
            }
        }

        // 2️⃣ Department‑level queue (no doctor assigned)
        return tokenRepository
                .findFirstByServiceTypeAndDoctorIsNullAndStatusOrderByCreatedAtAsc(
                        serviceType,
                        TokenStatus.WAITING
                )
                .orElseThrow(() -> new RuntimeException("No tokens in queue"));
    }

    /**
     * Waiting count for ETA / UI
     */
    public long getWaitingCount(Long serviceTypeId) {
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new RuntimeException("Service type not found"));

        return tokenRepository.countByServiceTypeAndStatus(
                serviceType,
                TokenStatus.WAITING
        );
    }

    /**
     * Auto‑assign available doctor (Phase‑4 polish)
     * Used when token is created WITHOUT doctor
     */
    @Transactional
    public void autoAssignDoctor(Token token) {

        Optional<Counter> availableDoctor =
                counterRepository.findFirstByDepartmentsContainsAndAvailability(
                        token.getServiceType(),
                        DoctorAvailability.AVAILABLE
                );

        if (availableDoctor.isPresent()) {
            Counter doctor = availableDoctor.get();
            token.setDoctor(doctor);
            doctor.setAvailability(DoctorAvailability.BUSY);
        }
    }
}

