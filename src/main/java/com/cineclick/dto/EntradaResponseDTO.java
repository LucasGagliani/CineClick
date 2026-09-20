package com.cineclick.dto;

import com.cineclick.model.EstadoEntrada;

import java.math.BigDecimal;

public record EntradaResponseDTO(
    Long id,
    String codigoQr,
    String fila,
    Integer numeroButaca,
    String codigoButaca,
    BigDecimal precioUnitario,
    EstadoEntrada estado
) {
}
