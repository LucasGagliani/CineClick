package com.cineclick.dto;

import com.cineclick.model.EstadoCompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResumenCompraDTO(
    Long compraId,
    String peliculaTitulo,
    LocalDateTime fechaHoraInicio,
    String cineNombre,
    String salaNombre,
    int cantidadEntradas,
    BigDecimal total,
    EstadoCompra estado,
    LocalDateTime fechaCompra,
    boolean elegibleArrepentimiento,
    String motivoNoElegible,
    String codigoArrepentimiento
) {
}
