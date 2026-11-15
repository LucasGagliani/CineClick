package com.cineclick.dto;

import com.cineclick.model.GeneroCliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DatosPersonalesRequestDTO(
    @NotBlank(message = "Ingresá tu nombre") @Size(max = 80) String nombre,
    @NotBlank(message = "Ingresá tu apellido") @Size(max = 80) String apellido,
    @Size(max = 40) String telefono,
    @Past(message = "La fecha de nacimiento tiene que ser anterior a hoy") LocalDate fechaNacimiento,
    GeneroCliente genero,
    Long cinePreferidoId
) {
}
