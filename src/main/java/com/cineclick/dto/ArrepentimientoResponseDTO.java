package com.cineclick.dto;

import com.cineclick.model.EstadoArrepentimiento;
import com.cineclick.model.MotivoArrepentimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ArrepentimientoResponseDTO(
    Long id,
    String codigo,
    EstadoArrepentimiento estado,
    LocalDateTime fecha,
    Long compraId,
    String peliculaTitulo,
    String nombre,
    String email,
    MotivoArrepentimiento motivo,
    String motivoFueraDePlazo,
    BigDecimal montoReembolso,
    LocalDateTime fechaReembolso
) {
}
