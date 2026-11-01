package com.cineclick.dto;

import com.cineclick.model.Clasificacion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Genero;
import com.cineclick.model.Idioma;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record PeliculaRequestDTO(
    @NotBlank(message = "Completá el título") @Size(max = 120) String titulo,
    @NotBlank(message = "Completá la sinopsis") @Size(max = 1200) String sinopsis,
    @NotNull(message = "Completá la duración") @Min(value = 1, message = "Tiene que durar entre 1 y 600 minutos") @Max(value = 600, message = "Tiene que durar entre 1 y 600 minutos") Integer duracionMinutos,
    @NotNull(message = "Elegí la calificación") Clasificacion clasificacion,
    @NotNull(message = "Elegí el género") Genero genero,
    @NotNull(message = "Elegí el idioma") Idioma idioma,
    @NotEmpty(message = "Elegí al menos un formato") Set<FormatoFuncion> formatosDisponibles,
    @Size(max = 500) String urlPoster,
    LocalDate fechaEstreno,
    Boolean destacada
) {
    public PeliculaRequestDTO(String titulo, String sinopsis, Integer duracionMinutos, Clasificacion clasificacion, Genero genero,
                              Idioma idioma, Set<FormatoFuncion> formatosDisponibles, String urlPoster, LocalDate fechaEstreno) {
        this(titulo, sinopsis, duracionMinutos, clasificacion, genero, idioma, formatosDisponibles, urlPoster, fechaEstreno, null);
    }
}
