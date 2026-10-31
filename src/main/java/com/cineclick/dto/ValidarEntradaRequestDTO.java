package com.cineclick.dto;

import jakarta.validation.constraints.NotNull;

public record ValidarEntradaRequestDTO(
    @NotNull Long funcionId
) {
}
