package com.cineclick.dto;

import com.cineclick.model.MotivoSoporte;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SoporteRequestDTO(
    @NotBlank(message = "Completá tu nombre") @Size(max = 120) String nombre,
    @NotBlank(message = "Completá el email") @Email(message = "Revisá el email") @Size(max = 120) String email,
    @NotNull(message = "Elegí el motivo") MotivoSoporte motivo,
    @Size(max = 30) String codigo,
    @NotBlank(message = "Escribí tu mensaje") @Size(min = 20, max = 1000, message = "El mensaje tiene que tener entre 20 y 1000 caracteres") String mensaje,
    @Size(max = 64) String claveIdempotencia
) {
}
