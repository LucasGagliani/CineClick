package com.cineclick.dto;

import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Genero;

import java.time.LocalDate;
import java.util.Set;

public record PeliculaResponseDTO(
    Long id,
    String titulo,
    String sinopsis,
    Integer duracionMinutos,
    String clasificacion,
    Genero genero,
    String idioma,
    Set<FormatoFuncion> formatosDisponibles,
    String urlPoster,
    LocalDate fechaEstreno,
    boolean activa
) {
}
