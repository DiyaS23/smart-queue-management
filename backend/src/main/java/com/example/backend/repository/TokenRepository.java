package com.example.backend.repository;

import com.example.backend.entity.Counter;
import com.example.backend.entity.Patient;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    // All waiting tokens for a service (FIFO + priority handled later)
    List<Token> findByPatientOrderByCreatedAtDesc(Patient patient);
//    List<Token> findByServiceTypeAndStatusOrderByCreatedAtAsc(
//            ServiceType serviceType,
//            TokenStatus status
//    );
    long countByServiceTypeAndStatusAndCreatedAtBefore(
            ServiceType serviceType,
            TokenStatus status,
            LocalDateTime createdAt
    );
    List<Token> findByStatus(TokenStatus status);
    Optional<Token> findByDoctorAndStatus(Counter doctor, TokenStatus status);


    // Count tokens ahead in queue
    long countByServiceTypeAndStatus(ServiceType serviceType, TokenStatus status);

    Optional<Token> findFirstByDoctorAndStatusOrderByCreatedAtAsc(
            Counter doctor,
            TokenStatus status
    );


    Optional<Token> findFirstByServiceTypeAndDoctorIsNullAndStatusOrderByCreatedAtAsc(
            ServiceType serviceType,
            TokenStatus status
    );

    long countByServiceType(ServiceType service);
}

