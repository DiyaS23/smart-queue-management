package com.example.backend.repository;

import com.example.backend.entity.Counter;
import com.example.backend.entity.enums.CounterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounterRepository extends JpaRepository<Counter, Long> {

    List<Counter> findByStatus(CounterStatus status);
}

