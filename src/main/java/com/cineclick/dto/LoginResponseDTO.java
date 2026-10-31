package com.cineclick.dto;

import com.cineclick.model.RolUsuario;

public record LoginResponseDTO(
    Long usuarioId,
    Long clienteId,
    String nombre,
    String apellido,
    String nombreCompleto,
    String email,
    RolUsuario rol,
    String mensaje
) {
}
