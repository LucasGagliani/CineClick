package com.cineclick.dto;

import com.cineclick.model.MetodoPago;
import jakarta.validation.constraints.NotNull;

public record PagoRequestDTO(
    @NotNull MetodoPago metodoPago,
    String datosPagoToken
) {
}
