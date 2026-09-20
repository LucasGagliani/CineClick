package com.cineclick.dto;

import com.cineclick.model.EstadoCompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompraResponseDTO(
    Long id,
    Long clienteId,
    String clienteNombre,
    Long funcionId,
    String peliculaTitulo,
    LocalDateTime fechaCompra,
    BigDecimal total,
    EstadoCompra estado,
    List<EntradaResponseDTO> entradas,
    PagoResponseDTO pago
) {
}
