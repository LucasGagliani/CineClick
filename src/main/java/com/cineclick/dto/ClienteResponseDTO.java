package com.cineclick.dto;

import java.time.LocalDateTime;

public record ClienteResponseDTO(
    Long id,
    Long usuarioId,
    String nombre,
    String apellido,
    String email,
    String telefono,
    LocalDateTime fechaRegistro,
    boolean activo
) {
}
