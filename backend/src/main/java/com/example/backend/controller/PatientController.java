package com.example.backend.controller;

import com.example.backend.dto.PatientTokenHistoryResponse;
import com.example.backend.service.PatientHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/history")
@RequiredArgsConstructor
public class PatientController {

    private final PatientHistoryService historyService;

    // 🔹 All visits
    @GetMapping
    public List<PatientTokenHistoryResponse> getHistory(
            @RequestParam String phone
    ) {
        return historyService.getPatientHistoryByPhone(phone);
    }

    // 🔹 Filter by department
    @GetMapping("/service/{serviceId}")
    public List<PatientTokenHistoryResponse> byService(
            @RequestParam String phone,
            @PathVariable Long serviceId
    ) {
        return historyService.filterByService(phone, serviceId);
    }

    // 🔹 Filter by doctor
    @GetMapping("/doctor/{doctorId}")
    public List<PatientTokenHistoryResponse> byDoctor(
            @RequestParam String phone,
            @PathVariable Long doctorId
    ) {
        return historyService.filterByDoctor(phone, doctorId);
    }
}

