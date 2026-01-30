package com.example.backend.repository;

import com.example.backend.entity.Counter;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.Token;
import com.example.backend.entity.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    // All waiting tokens for a service (FIFO + priority handled later)
    List<Token> findByServiceTypeAndStatusOrderByCreatedAtAsc(
            ServiceType serviceType,
            TokenStatus status
    );
    long countByServiceTypeAndStatusAndCreatedAtBefore(
            ServiceType serviceType,
            TokenStatus status,
            LocalDateTime createdAt
    );


    // Count tokens ahead in queue
    long countByServiceTypeAndStatus(ServiceType serviceType, TokenStatus status);

    // Current active token at a counter
    Optional<Token> findByCounterAndStatus(
            Counter counter,
            TokenStatus status
    );
}

