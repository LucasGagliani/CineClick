package com.cineclick.dto;

import com.cineclick.model.TipoEntrada;

import java.math.BigDecimal;

public record PrecioEntradaDTO(
    TipoEntrada tipo,
    String nombre,
    String seccion,
    String condiciones,
    BigDecimal precio
) {
}
