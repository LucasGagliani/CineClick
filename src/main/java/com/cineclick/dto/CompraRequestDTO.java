package com.cineclick.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompraRequestDTO(
    @NotNull Long clienteId,
    @NotNull Long funcionId,
    @NotEmpty List<Long> butacasIds,
    String codigoPromocion
) {
}
