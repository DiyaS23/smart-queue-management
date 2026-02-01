package com.example.backend.dto;

import com.example.backend.entity.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePatientDto {
    @NotBlank
    private String name;

    private Integer age;

    private Gender gender;

    @NotBlank
    private String phone;

    private String medicalId;
}

