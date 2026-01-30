package com.example.backend.controller;

import com.example.backend.entity.ServiceType;
import com.example.backend.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceTypeRepository serviceTypeRepository;

    @GetMapping
    public List<ServiceType> getServices() {
        return serviceTypeRepository.findAll();
    }
}

