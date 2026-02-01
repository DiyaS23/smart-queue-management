package com.example.backend.service;

import com.example.backend.dto.CreatePatientTokenRequest;
import com.example.backend.entity.Counter;
import com.example.backend.entity.Patient;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.PatientRepository;
import com.example.backend.repository.ServiceTypeRepository;
import com.example.backend.repository.TokenRepository;
import com.example.backend.websocket.QueueEvent;
import com.example.backend.websocket.QueueEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final QueueEventPublisher eventPublisher;
    private final PatientRepository patientRepository;
    private final CounterRepository counterRepository;
// Inside TokenService.java

    public List<Token> getTokensByStatus(TokenStatus status) {
        // Assuming you have a TokenRepository injected as 'tokenRepository'
        return tokenRepository.findByStatus(status);
    }
    @Transactional
    public Token createPatientToken(CreatePatientTokenRequest req) {
        Patient patient = patientRepository
                .findByPhone(req.getPatient().getPhone())
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setName(req.getPatient().getName());
                    p.setAge(req.getPatient().getAge());
                    p.setGender(req.getPatient().getGender());
                    p.setPhone(req.getPatient().getPhone());
                    p.setMedicalId(req.getPatient().getMedicalId());
                    return patientRepository.save(p);
                });

        ServiceType service = serviceTypeRepository.findById(req.getServiceTypeId())
                .orElseThrow(() -> new RuntimeException("Service not found"));

        Counter doctor = null;
        if (req.getDoctorId() != null) {
            doctor = counterRepository.findById(req.getDoctorId())
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            if (!doctor.getDepartments().contains(service)) {
                throw new RuntimeException("Doctor does not serve this department");
            }
        }

        Token token = new Token();
        token.setPatient(patient);
        token.setServiceType(service);
        token.setDoctor(doctor);
        token.setPriority(req.isPriority());
        token.setStatus(TokenStatus.WAITING);
        token.setCreatedAt(LocalDateTime.now());
        token.setTokenNumber(generateTokenNumber(service));

        return tokenRepository.save(token);
    }

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

