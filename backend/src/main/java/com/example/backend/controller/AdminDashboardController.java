package com.example.backend.controller;

import com.example.backend.dto.AdminDashboardSummary;
import com.example.backend.dto.DoctorLoadResponse;
import com.example.backend.dto.ServiceStatsResponse;
import com.example.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/summary")
    public AdminDashboardSummary summary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/doctors")
    public List<DoctorLoadResponse> doctorLoad() {
        return dashboardService.doctorLoad();
    }

    @GetMapping("/services")
    public List<ServiceStatsResponse> serviceStats() {
        return dashboardService.serviceStats();
    }
}

