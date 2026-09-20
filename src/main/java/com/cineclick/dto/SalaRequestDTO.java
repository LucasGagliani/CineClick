package com.cineclick.dto;

import com.cineclick.model.TipoSala;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SalaRequestDTO(
    @NotBlank @Size(max = 80) String nombre,
    @NotNull @Min(1) Integer numero,
    @NotNull TipoSala tipo,
    @NotNull @Min(1) Integer cantidadFilas,
    @NotNull @Min(1) Integer butacasPorFila
) {
}
