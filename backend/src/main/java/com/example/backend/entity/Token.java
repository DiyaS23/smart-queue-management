package com.example.backend.entity;

import com.example.backend.entity.enums.TokenPriority;
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
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status;

    private boolean priority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenPriority priorityType = TokenPriority.NORMAL;

    @Column(nullable = false)
    private boolean approved = true; // emergency approval


    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Counter doctor; // nullable (department queue)


    private LocalDateTime createdAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;

    @Version
    private Long version;
}

