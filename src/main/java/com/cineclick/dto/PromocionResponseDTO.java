package com.cineclick.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromocionResponseDTO(
    Long id,
    String codigo,
    String descripcion,
    BigDecimal porcentajeDescuento,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    boolean activa
) {
}
