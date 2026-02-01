package com.example.backend.entity;

import com.example.backend.entity.enums.CounterStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "counters")
public class Counter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name; // Counter 1, Counter 2

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounterStatus status;

//    @ManyToMany
//    @JoinTable(
//            name = "counter_services",
//            joinColumns = @JoinColumn(name = "counter_id"),
//            inverseJoinColumns = @JoinColumn(name = "service_id")
//    )
//    private Set<ServiceType> services = new HashSet<>();

    @Version
    private Long version;
    @ManyToMany
    @JoinTable(
            name = "doctor_departments",
            joinColumns = @JoinColumn(name = "counter_id"),
            inverseJoinColumns = @JoinColumn(name = "service_type_id")
    )
    private Set<ServiceType> departments = new HashSet<>();
    @Column(nullable = false)
    private Boolean available = true;

    private Integer maxPatientsPerDay;
}

