package com.cineclick.dto;

import com.cineclick.model.GeneroCliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClienteRequestDTO(
    @NotBlank @Size(max = 80) String nombre,
    @NotBlank @Size(max = 80) String apellido,
    @NotBlank @Email @Size(max = 120) String email,
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 y 72 caracteres")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "La contraseña tiene que tener al menos una letra y un número")
    String password,
    @NotBlank(message = "El teléfono es obligatorio") @Size(max = 40) String telefono,
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento tiene que ser anterior a hoy")
    LocalDate fechaNacimiento,
    @NotNull(message = "Elegí tu cine preferido") Long cinePreferidoId,
    @NotNull(message = "El género es obligatorio") GeneroCliente genero,
    Boolean aceptaTerminos
) {
}
