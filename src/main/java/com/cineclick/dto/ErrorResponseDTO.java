package com.cineclick.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDTO(
    LocalDateTime timestamp,
    int status,
    String error,
    String mensaje,
    String path,
    List<String> detalles
) {
}
