package com.example.backend.dto;

import com.example.backend.entity.enums.CounterStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CounterRequest {
    private String name;
    private CounterStatus status;
}
