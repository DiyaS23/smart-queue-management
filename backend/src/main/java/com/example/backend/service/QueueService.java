package com.example.backend.service;

import com.example.backend.entity.Counter;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenStatus;
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
    private final ServiceTypeRepository serviceTypeRepository;

    /**
     * Core queue algorithm:
     * 1. Priority tokens first
     * 2. FIFO within same priority
     */
//    @Transactional
//    public Token getNextToken(ServiceType serviceType) {
//        if (doctor != null) {
//            Optional<Token> doctorToken =
//                    tokenRepository.findFirstByDoctorAndStatusOrderByCreatedAtAsc(
//                            doctor, TokenStatus.WAITING
//                    );
//
//            if (doctorToken.isPresent()) {
//                return doctorToken.get();
//            }
//        }
//        return tokenRepository
//                .findFirstByServiceTypeAndDoctorIsNullAndStatusOrderByCreatedAtAsc(
//                        service, TokenStatus.WAITING
//                )
//                .orElseThrow(() -> new RuntimeException("No tokens in queue"));
        // 1️⃣ Priority tokens
//        List<Token> priorityTokens =
//                tokenRepository.findByServiceTypeAndStatusOrderByCreatedAtAsc(
//                                serviceType, TokenStatus.WAITING
//                        ).stream()
//                        .filter(Token::isPriority)
//                        .toList();
//
//        if (!priorityTokens.isEmpty()) {
//            return priorityTokens.get(0);
//        }
//
//        // 2️⃣ Normal FIFO
//        List<Token> normalTokens =
//                tokenRepository.findByServiceTypeAndStatusOrderByCreatedAtAsc(
//                                serviceType, TokenStatus.WAITING
//                        ).stream()
//                        .filter(t -> !t.isPriority())
//                        .toList();
//
//        if (normalTokens.isEmpty()) {
//            throw new RuntimeException("No tokens in queue");
//        }
//
//        return normalTokens.get(0);
//    }
    @Transactional
    public Token getNextToken(ServiceType serviceType, Counter doctor) {

        // 1️⃣ Doctor-specific queue
        if (doctor != null) {
            Optional<Token> doctorToken =
                    tokenRepository.findFirstByDoctorAndStatusOrderByCreatedAtAsc(
                            doctor, TokenStatus.WAITING
                    );

            if (doctorToken.isPresent()) {
                return doctorToken.get();
            }
        }

        // 2️⃣ Department queue
        return tokenRepository
                .findFirstByServiceTypeAndDoctorIsNullAndStatusOrderByCreatedAtAsc(
                        serviceType, TokenStatus.WAITING
                )
                .orElseThrow(() -> new RuntimeException("No tokens in queue"));
    }

    public long getWaitingCount(Long serviceTypeId) {
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new RuntimeException("Service type not found"));

        return tokenRepository.countByServiceTypeAndStatus(
                serviceType, TokenStatus.WAITING
        );
    }
}

