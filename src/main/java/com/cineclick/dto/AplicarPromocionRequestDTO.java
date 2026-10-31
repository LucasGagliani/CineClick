package com.cineclick.dto;

import jakarta.validation.constraints.NotBlank;

public record AplicarPromocionRequestDTO(
    @NotBlank String codigo
) {
}
