package com.cineclick.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ListaEsperaResponseDTO(
    Long peliculaId,
    String peliculaTitulo,
    LocalDate fechaEstreno,
    Long clienteId,
    LocalDateTime fechaAlta
) {
}
