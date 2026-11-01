package com.cineclick.dto;

import com.cineclick.model.Clasificacion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Genero;
import com.cineclick.model.Idioma;

import java.time.LocalDate;
import java.util.Set;

public record PeliculaResponseDTO(
    Long id,
    String titulo,
    String sinopsis,
    Integer duracionMinutos,
    Clasificacion clasificacion,
    Genero genero,
    Idioma idioma,
    Set<FormatoFuncion> formatosDisponibles,
    String urlPoster,
    LocalDate fechaEstreno,
    boolean activa,
    boolean destacada
) {
}
