package com.cineclick.dto;

import com.cineclick.model.PlanoSala;
import com.cineclick.model.TipoSala;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SalaRequestDTO(
    @NotBlank(message = "Completá el nombre") @Size(max = 80) String nombre,
    @NotNull(message = "Completá el número") @Min(value = 1, message = "Tiene que ser un número mayor a 0") Integer numero,
    @NotNull(message = "Elegí el tipo de sala") TipoSala tipo,
    @Min(value = 1, message = "Tiene que haber al menos 1 fila") @Max(value = 26, message = "Puede haber hasta 26 filas") Integer cantidadFilas,
    @Min(value = 1, message = "Tiene que haber al menos 1 butaca por fila") @Max(value = 40, message = "Puede haber hasta 40 butacas por fila") Integer butacasPorFila,
    PlanoSala plano
) {
}
