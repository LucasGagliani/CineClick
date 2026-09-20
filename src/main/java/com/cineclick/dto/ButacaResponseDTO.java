package com.cineclick.dto;

import com.cineclick.model.TipoButaca;

public record ButacaResponseDTO(
    Long id,
    String fila,
    Integer numero,
    String codigo,
    TipoButaca tipo,
    boolean disponible
) {
}
