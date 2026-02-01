package com.example.backend.service;

import com.example.backend.dto.PatientTokenHistoryResponse;
import com.example.backend.entity.Counter;
import com.example.backend.entity.Patient;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenPriority;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.PatientRepository;
import com.example.backend.repository.ServiceTypeRepository;
import com.example.backend.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class PatientHistoryService {

    private final PatientRepository patientRepository;
    private final TokenRepository tokenRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final CounterRepository counterRepository;

    public List<PatientTokenHistoryResponse> getPatientHistoryByPhone(String phone) {
        Patient patient = patientRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        return tokenRepository.findByPatientOrderByCreatedAtDesc(patient)
                .stream()
                .map(this::map)
                .toList();
    }

    public List<PatientTokenHistoryResponse> filterByService(
            String phone,
            Long serviceId
    ) {
        Patient patient = patientRepository.findByPhone(phone).orElseThrow();
        ServiceType service = serviceTypeRepository.findById(serviceId).orElseThrow();

        return tokenRepository.findByPatientAndServiceType(patient, service)
                .stream()
                .map(this::map)
                .toList();
    }

    public List<PatientTokenHistoryResponse> filterByDoctor(
            String phone,
            Long doctorId
    ) {
        Patient patient = patientRepository.findByPhone(phone).orElseThrow();
        Counter doctor = counterRepository.findById(doctorId).orElseThrow();

        return tokenRepository.findByPatientAndDoctor(patient, doctor)
                .stream()
                .map(this::map)
                .toList();
    }

    private PatientTokenHistoryResponse map(Token token) {
        return new PatientTokenHistoryResponse(
                token.getId(),
                token.getTokenNumber(),
                token.getServiceType().getName(),
                token.getDoctor() != null ? token.getDoctor().getName() : null,
                token.getStatus().name(),
                token.getPriorityType() == TokenPriority.URGENT,
                token.getCreatedAt(),
                token.getCalledAt(),
                token.getCompletedAt()
        );
    }
}
