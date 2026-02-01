package com.example.backend.dto;

import jakarta.validation.Valid;
//import org.antlr.v4.runtime.misc.NotNull;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTokenRequest {
//    private Long serviceTypeId;
//    private boolean priority;

    @Valid
    @NotNull
    private CreatePatientDto patient;

    @NotNull
    private Long serviceId;   // OPD / department

    private Long doctorId;    // optional

    private boolean priority;
}
