package com.example.backend.controller;

import com.example.backend.dto.PatientTokenHistoryResponse;
import com.example.backend.service.PatientHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientHistoryService historyService;

    // Public (patients don’t login)
    @GetMapping("/history")
    public ResponseEntity<List<PatientTokenHistoryResponse>> getHistory(
            @RequestParam String phone) {

        return ResponseEntity.ok(historyService.getHistoryByPhone(phone));
    }
}

