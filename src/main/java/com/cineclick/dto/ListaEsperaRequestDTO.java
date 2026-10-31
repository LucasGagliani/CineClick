package com.cineclick.dto;

import jakarta.validation.constraints.NotNull;

public record ListaEsperaRequestDTO(
    @NotNull Long clienteId
) {
}
