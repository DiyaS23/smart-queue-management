package com.example.backend.repository;

import com.example.backend.entity.Counter;
import com.example.backend.entity.Patient;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenPriority;
import com.example.backend.entity.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    List<Token> findByPatientAndServiceType(
            Patient patient,
            ServiceType serviceType
    );

    List<Token> findByPatientAndDoctor(
            Patient patient,
            Counter doctor
    );

    List<Token> findByPatientOrderByCreatedAtDesc(Patient patient);
    long countByServiceTypeAndStatusAndCreatedAtBefore(
            ServiceType serviceType,
            TokenStatus status,
            LocalDateTime createdAt
    );
    List<Token> findByStatus(TokenStatus status);
    long countByServiceTypeAndStatus(ServiceType serviceType, TokenStatus status);

    Optional<Token> findFirstByDoctorAndStatusOrderByCreatedAtAsc(
            Counter doctor,
            TokenStatus status
    );


    Optional<Token> findFirstByServiceTypeAndDoctorIsNullAndStatusOrderByCreatedAtAsc(
            ServiceType serviceType,
            TokenStatus status
    );
    long countByStatus(TokenStatus status);

    long countByStatusAndPriorityType(
            TokenStatus status,
            TokenPriority priorityType
    );

    long countByStatusAndCompletedAtBetween(
            TokenStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByDoctorAndStatus(
            Counter doctor,
            TokenStatus status
    );

    long countByDoctorAndStatusAndCompletedAtBetween(
            Counter doctor,
            TokenStatus status,
            LocalDateTime start,
            LocalDateTime end
    );


    Optional<Token> findFirstByStatusAndApprovedAndPriorityTypeOrderByCreatedAtAsc(TokenStatus tokenStatus, boolean b, TokenPriority tokenPriority);

    List<Token> findByPriorityTypeAndApprovedFalse(TokenPriority tokenPriority);
}
