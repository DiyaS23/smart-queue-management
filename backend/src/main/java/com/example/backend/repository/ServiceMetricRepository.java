package com.example.backend.repository;

import com.example.backend.entity.ServiceMetric;
import com.example.backend.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceMetricRepository
        extends JpaRepository<ServiceMetric, Long> {

    Optional<ServiceMetric> findByServiceType(ServiceType serviceType);
}

