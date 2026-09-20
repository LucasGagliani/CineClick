package com.cineclick.dto;

import com.cineclick.model.RolUsuario;

public record LoginResponseDTO(
    Long usuarioId,
    String nombreCompleto,
    String email,
    RolUsuario rol,
    String mensaje
) {
}
