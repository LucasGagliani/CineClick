package com.cineclick.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoRequestDTO(
    @NotNull Boolean activo
) {
}
