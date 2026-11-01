package com.cineclick.dto;

import com.cineclick.model.TipoSala;

public record SalaResponseDTO(
    Long id,
    Long cineId,
    String cineNombre,
    String nombre,
    Integer numero,
    TipoSala tipo,
    boolean activa,
    Integer capacidad
) {
}
