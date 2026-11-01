package com.cineclick.dto;

import com.cineclick.model.Clasificacion;
import com.cineclick.model.EstadoFuncion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Idioma;
import com.cineclick.model.Provincia;

import java.time.LocalDateTime;

public record FuncionCompraDTO(
    Long id,
    LocalDateTime fechaHoraInicio,
    LocalDateTime fechaHoraFin,
    FormatoFuncion formato,
    Idioma idioma,
    EstadoFuncion estado,
    Long peliculaId,
    String peliculaTitulo,
    String urlPoster,
    Clasificacion clasificacion,
    Long cineId,
    String cineNombre,
    String salaNombre,
    Provincia provincia,
    String localidad,
    String calle,
    Integer altura
) {
}
