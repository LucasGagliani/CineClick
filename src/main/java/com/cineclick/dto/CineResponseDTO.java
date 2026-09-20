package com.cineclick.dto;

import java.util.List;

public record CineResponseDTO(
    Long id,
    String nombre,
    String direccion,
    String ciudad,
    String telefono,
    boolean activo,
    List<SalaResponseDTO> salas
) {
}
