package com.cineclick.dto;

import com.cineclick.model.FormatoFuncion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuncionRequestDTO(
    @NotNull Long peliculaId,
    @NotNull Long salaId,
    @NotNull @Future LocalDateTime fechaHoraInicio,
    @NotNull @DecimalMin("0.01") BigDecimal precioBase,
    @NotNull FormatoFuncion formato,
    @NotBlank @Size(max = 40) String idioma
) {
}
