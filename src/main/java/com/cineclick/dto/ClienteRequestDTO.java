package com.cineclick.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequestDTO(
    @NotBlank @Size(max = 80) String nombre,
    @NotBlank @Size(max = 80) String apellido,
    @NotBlank @Email @Size(max = 120) String email,
    @NotBlank @Size(min = 4, max = 120) String password,
    @Size(max = 40) String telefono
) {
}
