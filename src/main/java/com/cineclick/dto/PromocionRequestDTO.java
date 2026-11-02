package com.cineclick.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

public record PromocionRequestDTO(
    @NotBlank(message = "Completá el código") @Size(max = 40) @Pattern(regexp = "^\\s*[A-Za-z0-9]+\\s*$", message = "Solo letras y números, sin espacios") String codigo,
    @Size(max = 60) String titulo,
    @Size(max = 200) String descripcion,
    @NotNull(message = "Completá el descuento") @DecimalMin(value = "1", message = "Tiene que estar entre 1 y 100") @DecimalMax(value = "100", message = "Tiene que estar entre 1 y 100") @Digits(integer = 3, fraction = 2, message = "Puede tener hasta 2 decimales") BigDecimal porcentajeDescuento,
    @NotNull(message = "Completá desde cuándo vale") LocalDateTime vigenciaDesde,
    @NotNull(message = "Completá hasta cuándo vale") LocalDateTime vigenciaHasta,
    Set<DayOfWeek> diasSemana,
    @Min(value = 1, message = "Tiene que ser un número mayor a 0") Integer usosMaximosTotales,
    @Min(value = 1, message = "Tiene que ser un número mayor a 0") Integer usosMaximosPorUsuario,
    boolean soloPrimeraCompra,
    @Min(value = 1, message = "Tiene que ser un número mayor a 0") Integer maxEntradasConDescuento,
    @DecimalMin(value = "0.01", message = "Tiene que ser mayor a 0") @Digits(integer = 10, fraction = 2, message = "Puede tener hasta 2 decimales") BigDecimal montoMaximoDescuento,
    boolean activa
) {
}
