package com.cineclick.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromocionRequestDTO(
    @NotBlank @Size(max = 40) String codigo,
    @NotBlank @Size(max = 200) String descripcion,
    @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal porcentajeDescuento,
    @NotNull LocalDate fechaInicio,
    @NotNull LocalDate fechaFin,
    boolean activa
) {
}
