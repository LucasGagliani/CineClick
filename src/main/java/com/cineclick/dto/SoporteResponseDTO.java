package com.cineclick.dto;

import com.cineclick.model.EstadoConsulta;
import com.cineclick.model.MotivoSoporte;

import java.time.LocalDateTime;

public record SoporteResponseDTO(
    Long id,
    String numero,
    String nombre,
    String email,
    MotivoSoporte motivo,
    Long compraId,
    String mensaje,
    LocalDateTime fecha,
    EstadoConsulta estado
) {
}
