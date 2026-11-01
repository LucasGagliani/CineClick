package com.cineclick.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromocionRequestDTO(
    @NotBlank(message = "Completá el código") @Size(max = 40) @Pattern(regexp = "^\\s*[A-Za-z0-9]+\\s*$", message = "Solo letras y números, sin espacios") String codigo,
    @Size(max = 60) String titulo,
    @NotBlank(message = "Completá las condiciones") @Size(max = 200) String descripcion,
    @NotNull(message = "Completá el descuento") @DecimalMin(value = "0.01", message = "Tiene que estar entre 0,01 y 100") @DecimalMax(value = "100.00", message = "Tiene que estar entre 0,01 y 100") @Digits(integer = 3, fraction = 2, message = "Puede tener hasta 2 decimales") BigDecimal porcentajeDescuento,
    @NotNull(message = "Completá la fecha de inicio") LocalDate fechaInicio,
    @NotNull(message = "Completá la fecha de fin") LocalDate fechaFin,
    boolean activa
) {
}
