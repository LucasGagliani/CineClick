package com.cineclick.dto;

import com.cineclick.model.Provincia;

import java.util.List;

public record CineResponseDTO(
    Long id,
    String nombre,
    Provincia provincia,
    String localidad,
    String calle,
    Integer altura,
    String telefono,
    String urlImagen,
    boolean activo,
    List<SalaResponseDTO> salas
) {
}
