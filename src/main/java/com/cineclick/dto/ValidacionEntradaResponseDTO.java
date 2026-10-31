package com.cineclick.dto;

import com.cineclick.model.ResultadoValidacion;

import java.time.LocalDateTime;

public record ValidacionEntradaResponseDTO(
    ResultadoValidacion resultado,
    String codigo,
    Long funcionId,
    String peliculaTitulo,
    String cineNombre,
    String salaNombre,
    LocalDateTime fechaHoraInicio,
    String codigoButaca
) {
}
