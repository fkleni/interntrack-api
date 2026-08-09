package com.interntrack.api.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
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
