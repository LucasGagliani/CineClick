package com.cineclick.dto;

import com.cineclick.model.EstadoEntrada;
import com.cineclick.model.TipoEntrada;

import java.math.BigDecimal;

public record EntradaResponseDTO(
    Long id,
    String codigoQr,
    String fila,
    Integer numeroButaca,
    String codigoButaca,
    TipoEntrada tipo,
    BigDecimal precioUnitario,
    BigDecimal descuento,
    EstadoEntrada estado
) {
}
