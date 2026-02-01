package com.example.backend.service;

import com.example.backend.dto.PatientTokenHistoryResponse;
import com.example.backend.entity.Patient;
import com.example.backend.repository.PatientRepository;
import com.example.backend.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientHistoryService {

    private final PatientRepository patientRepository;
    private final TokenRepository tokenRepository;

    public List<PatientTokenHistoryResponse> getHistoryByPhone(String phone) {

        Patient patient = patientRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        return tokenRepository.findByPatientOrderByCreatedAtDesc(patient)
                .stream()
                .map(token -> new PatientTokenHistoryResponse(
                        token.getTokenNumber(),
                        token.getServiceType().getName(),
                        token.getDoctor() != null ? token.getDoctor().getName() : null,
                        token.getStatus().name(),
                        token.getCreatedAt(),
                        token.getCompletedAt()
                ))
                .toList();
    }
}

