package com.example.backend.service;

import com.example.backend.dto.CreatePatientDto;
import com.example.backend.dto.CreateTokenRequest;
import com.example.backend.dto.TokenResponse;
import com.example.backend.entity.Counter;
import com.example.backend.entity.Patient;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenStatus;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.PatientRepository;
import com.example.backend.repository.ServiceTypeRepository;
import com.example.backend.repository.TokenRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;

//@Service
//@RequiredArgsConstructor
//public class TokenService {
//
//    private final TokenRepository tokenRepository;
//    private final ServiceTypeRepository serviceTypeRepository;
//
//    @Transactional
//    public Token createToken(Long serviceTypeId, boolean priority) {
//        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
//                .orElseThrow(() -> new RuntimeException("Service not found"));
//
//        Token token = new Token();
//        token.setServiceType(serviceType);
//        token.setPriority(priority);
//        token.setStatus(TokenStatus.WAITING);
//        token.setCreatedAt(LocalDateTime.now());
//        token.setTokenNumber(generateTokenNumber(serviceType));
//
//        return tokenRepository.save(token);
//    }
//
//    private String generateTokenNumber(ServiceType serviceType) {
//        // Example: CASH → C101, DOCTOR → D205
//        String prefix = serviceType.getName().substring(0, 1).toUpperCase();
//        long count = tokenRepository.count();
//        return prefix + (100 + count);
//    }
//
//    @Transactional
//    public void updateStatus(Token token, TokenStatus status) {
//        token.setStatus(status);
//        if (status == TokenStatus.CALLED) {
//            token.setCalledAt(LocalDateTime.now());
//        }
//        if (status == TokenStatus.COMPLETED) {
//            token.setCompletedAt(LocalDateTime.now());
//        }
//        tokenRepository.save(token);
//    }
//}

@Service
@RequiredArgsConstructor
public class TokenService {

    private final PatientRepository patientRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final CounterRepository counterRepository;
    private final TokenRepository tokenRepository;

    @Transactional
    public TokenResponse createToken(CreateTokenRequest req) {

        // 1. Resolve or create patient (by phone)
        Patient patient = patientRepository
                .findByPhone(req.getPatient().getPhone())
                .orElseGet(() -> patientRepository.save(mapPatient(req.getPatient())));

        // 2. Validate service (department)
        ServiceType service = serviceTypeRepository.findById(req.getServiceId())
                .orElseThrow(() -> new RuntimeException("Service not found"));

        // 3. Optional doctor validation
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

        // 4. Create token
        Token token = new Token();
        token.setPatient(patient);
        token.setServiceType(service);
        token.setDoctor(doctor);
        token.setPriority(req.isPriority());
        token.setStatus(TokenStatus.WAITING);
        token.setTokenNumber(generateTokenNumber(service));
        token.setCreatedAt(LocalDateTime.now());

        Token saved = tokenRepository.save(token);

        TokenResponse res = new TokenResponse();
        res.setId(saved.getId());
        res.setTokenNumber(saved.getTokenNumber());
        res.setServiceName(service.getName());
        res.setStatus(saved.getStatus().name());
        res.setPriority(saved.isPriority());
        res.setCreatedAt(saved.getCreatedAt());
        res.setTokenId(saved.getId());
        res.setDoctorName(doctor != null ? doctor.getName() : null);
        return res ;
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

    private String generateTokenNumber(ServiceType service) {
        // simple version (can improve later)
        long count = tokenRepository.countByServiceType(service) + 1;
        return service.getName().substring(0, 1).toUpperCase() + count;
    }
}

