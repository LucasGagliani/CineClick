package com.cineclick.dto;

import com.cineclick.model.MotivoFueraDeHorario;
import com.cineclick.model.ResultadoValidacion;
import com.cineclick.model.TipoEntrada;

import java.time.LocalDateTime;

public record ValidacionEntradaResponseDTO(
    ResultadoValidacion resultado,
    MotivoFueraDeHorario motivo,
    String codigo,
    String peliculaTitulo,
    Long cineId,
    String cineNombre,
    String salaNombre,
    String codigoButaca,
    LocalDateTime fechaHoraInicio,
    TipoEntrada tipo,
    LocalDateTime usadaEn,
    LocalDateTime ingresoDesde,
    LocalDateTime ingresoHasta
) {
}
