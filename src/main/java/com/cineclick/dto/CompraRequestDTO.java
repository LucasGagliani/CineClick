package com.cineclick.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompraRequestDTO(
    @NotNull Long clienteId,
    @NotNull Long funcionId,
    @NotEmpty @Size(max = 10, message = "no se pueden reservar mas de 10 butacas") List<Long> butacasIds,
    String codigoPromocion
) {
}
