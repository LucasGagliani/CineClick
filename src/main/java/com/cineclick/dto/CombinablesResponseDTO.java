package com.cineclick.dto;

import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Idioma;

import java.time.LocalDateTime;
import java.util.List;

public record CombinablesResponseDTO(
    Referencia elegida,
    List<Combinable> antes,
    List<Combinable> despues
) {

    public record Referencia(Long funcionId, String pelicula, LocalDateTime inicio, LocalDateTime finEstimado) {
    }

    public record Combinable(
        Long funcionId,
        Long peliculaId,
        String pelicula,
        String urlPoster,
        FormatoFuncion formato,
        Idioma idioma,
        String sala,
        LocalDateTime inicio,
        LocalDateTime finEstimado,
        long minutosEntre,
        int butacasLibres
    ) {
    }
}
