package com.interntrack.api.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "applications")
@Data
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Company name cannot be blank")
    private String companyName;

    @NotBlank(message = "Position cannot be blank")
    private String position;

    @NotBlank(message = "Status cannot be blank")
    private String status;

    @NotNull(message = "Applied date cannot be null")
    @PastOrPresent(message = "Applied date cannot be in the future")
    private LocalDate appliedDate;

    private String notes;
}
