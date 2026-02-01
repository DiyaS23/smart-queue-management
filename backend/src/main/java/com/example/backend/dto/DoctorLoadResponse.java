package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DoctorLoadResponse {
    private Long doctorId;
    private String doctorName;
    private long waitingCount;
    private long servingCount;
    private long completedToday;
}
