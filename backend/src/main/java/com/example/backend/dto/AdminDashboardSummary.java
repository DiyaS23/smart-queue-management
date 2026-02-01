package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminDashboardSummary {
    private long tokensServedToday;
    private long waitingTokens;
    private long emergencyPending;
    private long emergencyApproved;
}

