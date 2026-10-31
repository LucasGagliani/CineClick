package com.cineclick.dto;

import com.cineclick.model.TipoEntrada;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EntradaSolicitadaDTO(
    @NotNull TipoEntrada tipo,
    @NotNull @Min(1) Integer cantidad
) {
}
