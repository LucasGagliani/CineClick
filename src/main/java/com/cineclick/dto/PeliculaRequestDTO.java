package com.cineclick.dto;

import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Genero;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record PeliculaRequestDTO(
    @NotBlank @Size(max = 120) String titulo,
    @NotBlank @Size(max = 1200) String sinopsis,
    @NotNull @Min(1) Integer duracionMinutos,
    @NotBlank @Size(max = 20) String clasificacion,
    @NotNull Genero genero,
    @NotBlank @Size(max = 40) String idioma,
    @NotEmpty Set<FormatoFuncion> formatosDisponibles,
    @Size(max = 500) String urlPoster,
    LocalDate fechaEstreno
) {
}
