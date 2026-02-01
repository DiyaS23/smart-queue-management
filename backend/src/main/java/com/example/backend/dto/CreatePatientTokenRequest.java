package com.example.backend.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePatientTokenRequest {

    @Valid
    @NotNull
    private CreatePatientDto patient;

    @NotNull
    private Long serviceTypeId;

    private Long doctorId; // optional
    private boolean urgent; // instead of generic priority


}

