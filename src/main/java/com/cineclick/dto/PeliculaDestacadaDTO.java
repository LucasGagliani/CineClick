package com.cineclick.dto;

import com.cineclick.model.CategoriaDestacada;

public record PeliculaDestacadaDTO(
    PeliculaResponseDTO pelicula,
    CategoriaDestacada categoria
) {
}
