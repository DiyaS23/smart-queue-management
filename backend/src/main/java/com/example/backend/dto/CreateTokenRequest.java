package com.example.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTokenRequest {
//    private Long serviceTypeId;
//    private boolean priority;


    @NotNull
    private Long serviceId;   // OPD / department

    private boolean priority;

}
