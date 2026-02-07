package com.example.backend.service;

import com.example.backend.dto.CreatePatientDto;
import com.example.backend.dto.CreatePatientTokenRequest;
import com.example.backend.dto.CreateTokenRequest;
import com.example.backend.dto.TokenResponse;
import com.example.backend.entity.Counter;
import com.example.backend.entity.Patient;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.DoctorAvailability;
import com.example.backend.entity.enums.TokenPriority;
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
import java.util.Optional;

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
    public Token createToken(Long serviceTypeId, boolean priority) {

        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        Token token = new Token();
        token.setServiceType(serviceType);
        token.setPriority(priority);
        token.setPriorityType(TokenPriority.NORMAL);
        token.setApproved(true);
        token.setStatus(TokenStatus.WAITING);
        token.setCreatedAt(LocalDateTime.now());
        token.setTokenNumber(generateTokenNumber(serviceType));

        Token saved = tokenRepository.save(token);

        eventPublisher.publishQueueUpdate(
                new QueueEvent("TOKEN_CREATED",
                        saved.getTokenNumber(),
                        null,
                        serviceType.getName(),
                        saved.getStatus().name())
        );

        return saved;
    }

    // -------------------------------
    // NEW HOSPITAL FLOW
    // -------------------------------
//    @Transactional
//    public Token createPatientToken(CreatePatientTokenRequest req) {
//
//        Patient patient = patientRepository
//                .findByPhone(req.getPatient().getPhone())
//                .orElseGet(() -> patientRepository.save(mapPatient(req.getPatient())));
//
//        ServiceType service = serviceTypeRepository.findById(req.getServiceTypeId())
//                .orElseThrow(() -> new RuntimeException("Service not found"));
//
//        Counter doctor = null;
//        if (req.getDoctorId() != null) {
//            doctor = counterRepository.findById(req.getDoctorId())
//                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
//
//            if (!doctor.getDepartments().contains(service)) {
//                throw new RuntimeException("Doctor does not serve this department");
//            }
//
//            if (!Boolean.TRUE.equals(doctor.getAvailable())) {
//                throw new RuntimeException("Doctor not available");
//            }
//        }
//
//        Token token = new Token();
//        token.setPatient(patient);
//        token.setServiceType(service);
//        token.setDoctor(doctor);
//        token.setCreatedAt(LocalDateTime.now());
//        token.setTokenNumber(generateTokenNumber(service));
//
//        if (req.isUrgent()) {
//            token.setPriorityType(TokenPriority.URGENT);
//            token.setApproved(false);
//            token.setStatus(TokenStatus.PENDING_APPROVAL);
//            Token saved = tokenRepository.save(token);
//
//            // 🔔 REAL‑TIME ADMIN NOTIFICATION
//            eventPublisher.publishQueueUpdate(
//                    new QueueEvent(
//                            "EMERGENCY_CREATED",
//                            saved.getTokenNumber(),
//                            null,
//                            service.getName(),
//                            "PENDING_APPROVAL"
//                    )
//            );
//
//            return saved;
//        }
//        token.setPriorityType(TokenPriority.NORMAL);
//        token.setApproved(true);
//        token.setStatus(TokenStatus.WAITING);
//        token.setPriority(req.isUrgent());
//
//        Token saved = tokenRepository.save(token);
//        return saved;
//    }
    @Transactional
    public Token createPatientToken(CreatePatientTokenRequest req) {

        Patient patient = patientRepository
                .findByPhone(req.getPatient().getPhone())
                .orElseGet(() -> patientRepository.save(mapPatient(req.getPatient())));

        ServiceType service = serviceTypeRepository.findById(req.getServiceTypeId())
                .orElseThrow(() -> new RuntimeException("Service not found"));

        Counter doctor = null;
        if (req.getDoctorId() != null) {
            doctor = counterRepository.findById(req.getDoctorId())
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            if (!doctor.getDepartments().contains(service)) {
                throw new RuntimeException("Doctor does not serve this department");
            }

            if (!Boolean.TRUE.equals(doctor.getAvailable())) {
                throw new RuntimeException("Doctor not available");
            }
        }

        Token token = new Token();
        token.setPatient(patient);
        token.setServiceType(service);
        token.setDoctor(doctor);
        token.setCreatedAt(LocalDateTime.now());
        token.setTokenNumber(generateTokenNumber(service));

        // 🔴 EMERGENCY FLOW (FIXED)
        if (req.isUrgent()) {
            token.setPriority(true); // ✅ REQUIRED
            token.setPriorityType(TokenPriority.URGENT);
            token.setApproved(false);
            token.setStatus(TokenStatus.PENDING_APPROVAL);

            Token saved = tokenRepository.save(token);

            eventPublisher.publishQueueUpdate(
                    new QueueEvent(
                            "EMERGENCY_CREATED",
                            saved.getTokenNumber(),
                            null,
                            service.getName(),
                            TokenStatus.PENDING_APPROVAL.name()
                    )
            );

            return saved;
        }

        // 🟢 NORMAL FLOW
        token.setPriority(false);
        token.setPriorityType(TokenPriority.NORMAL);
        token.setApproved(true);
        token.setStatus(TokenStatus.WAITING);

        return tokenRepository.save(token);
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
    @Transactional
    public Token approveEmergency(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        if (token.getPriorityType() != TokenPriority.URGENT) {
            throw new RuntimeException("Not an emergency token");
        }
        token.setApproved(true);
        token.setStatus(TokenStatus.WAITING);
        Optional<Counter> freeDoctor =
                counterRepository.findFirstByDepartmentsContainsAndAvailability(
                        token.getServiceType(),
                        DoctorAvailability.AVAILABLE
                );

        if (freeDoctor.isPresent()) {
            Counter doctor = freeDoctor.get();

            token.setDoctor(doctor);
            doctor.setAvailability(DoctorAvailability.BUSY);
            counterRepository.save(doctor);
        }

        Token saved = tokenRepository.save(token);
        QueueEvent event = new QueueEvent(
                "EMERGENCY_APPROVED",
                token.getTokenNumber(),
                null,
                token.getServiceType().getName(),
                "WAITING"
        );
        eventPublisher.publishQueueUpdate(event);
        eventPublisher.publishToPatient(
                token.getTokenNumber(),
                event
        );
        return saved;
    }
    @Transactional
    public void rejectEmergency(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (token.getPriorityType() != TokenPriority.URGENT) {
            throw new RuntimeException("Not an emergency token");
        }

        token.setApproved(false);
        token.setStatus(TokenStatus.CANCELLED);

        tokenRepository.save(token);

        eventPublisher.publishQueueUpdate(
                new QueueEvent(
                        "EMERGENCY_REJECTED",
                        token.getTokenNumber(),
                        null,
                        token.getServiceType().getName(),
                        token.getStatus().name()
                )
        );
    }

    private Patient mapPatient(CreatePatientDto dto) {
        Patient p = new Patient();
        p.setName(dto.getName());
        p.setAge(dto.getAge());
        p.setGender(dto.getGender());
        p.setPhone(dto.getPhone());
        p.setMedicalId(dto.getMedicalId());
        return p;
    }
}

