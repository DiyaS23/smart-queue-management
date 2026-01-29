package com.example.backend.entity;

import com.example.backend.entity.enums.TokenStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tokenNumber; // A101, B202

    @ManyToOne
    @JoinColumn(name = "service_id")
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    private TokenStatus status;

    private boolean priority;

    @ManyToOne
    @JoinColumn(name = "counter_id")
    private Counter counter;

    private LocalDateTime createdAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;

    @Version
    private Long version;
}

