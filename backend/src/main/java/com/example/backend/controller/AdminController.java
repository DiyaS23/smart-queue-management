package com.example.backend.controller;

import com.example.backend.dto.CounterRequest;
import com.example.backend.entity.Counter;
import com.example.backend.entity.ServiceType;
import com.example.backend.repository.CounterRepository;
import com.example.backend.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

import java.util.List;
@Data
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CounterRepository counterRepository;
    private final ServiceTypeRepository serviceTypeRepository;

    @PostMapping("/services")
    public ServiceType createService(@RequestBody ServiceType serviceType) {
        return serviceTypeRepository.save(serviceType);
    }

    @PostMapping("/counters")
    public Counter createCounter(@RequestBody CounterRequest request) {
        Counter counter = new Counter();
        counter.setName(request.getName());
        counter.setStatus(request.getStatus());
        return counterRepository.save(counter);
    }

    @GetMapping("/counters")
    public List<Counter> getCounters() {
        return counterRepository.findAll();
    }
}
