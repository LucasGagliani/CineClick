package com.cineclick.dto;

import com.cineclick.model.GeneroCliente;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteResponseDTO(
    Long id,
    Long usuarioId,
    String nombre,
    String apellido,
    String email,
    String telefono,
    LocalDateTime fechaRegistro,
    boolean activo,
    LocalDate fechaNacimiento,
    GeneroCliente genero,
    Long cinePreferidoId,
    String cinePreferidoNombre
) {
}
