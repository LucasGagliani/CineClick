package com.cineclick.dto;

import com.cineclick.model.EstadoCompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompraAdminDTO(
    Long id,
    LocalDateTime fechaCompra,
    String clienteNombre,
    String clienteEmail,
    String peliculaTitulo,
    String cineNombre,
    String salaNombre,
    LocalDateTime fechaHoraFuncion,
    int cantidadEntradas,
    BigDecimal total,
    EstadoCompra estado,
    String codigoPromocion
) {
}
