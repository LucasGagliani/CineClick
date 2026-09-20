package com.cineclick.dto;

import com.cineclick.model.EstadoFuncion;
import com.cineclick.model.FormatoFuncion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuncionResponseDTO(
    Long id,
    Long peliculaId,
    String peliculaTitulo,
    Long cineId,
    String cineNombre,
    Long salaId,
    String salaNombre,
    LocalDateTime fechaHoraInicio,
    LocalDateTime fechaHoraFin,
    BigDecimal precioBase,
    FormatoFuncion formato,
    String idioma,
    EstadoFuncion estado
) {
}
