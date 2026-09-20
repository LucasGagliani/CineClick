package com.cineclick.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CineRequestDTO(
    @NotBlank @Size(max = 120) String nombre,
    @NotBlank @Size(max = 160) String direccion,
    @NotBlank @Size(max = 80) String ciudad,
    @Size(max = 40) String telefono
) {
}
