package com.cineclick.dto;

import com.cineclick.model.EstadoPago;
import com.cineclick.model.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponseDTO(
    Long id,
    BigDecimal monto,
    MetodoPago metodoPago,
    EstadoPago estado,
    LocalDateTime fechaPago,
    String codigoTransaccion
) {
}
