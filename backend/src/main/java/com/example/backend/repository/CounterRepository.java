package com.example.backend.repository;

import com.example.backend.entity.Counter;
import com.example.backend.entity.ServiceType;
import com.example.backend.entity.enums.CounterStatus;
import com.example.backend.entity.enums.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CounterRepository extends JpaRepository<Counter, Long> {

    List<Counter> findByStatus(CounterStatus status);

    Optional<Counter> findFirstByDepartmentsContainsAndAvailability(ServiceType serviceType, DoctorAvailability doctorAvailability);
}

