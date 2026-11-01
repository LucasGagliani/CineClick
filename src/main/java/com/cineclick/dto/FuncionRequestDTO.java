package com.cineclick.dto;

import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Idioma;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuncionRequestDTO(
    @NotNull(message = "Elegí la película") Long peliculaId,
    @NotNull(message = "Elegí la sala") Long salaId,
    @NotNull(message = "Completá la fecha y la hora") LocalDateTime fechaHoraInicio,
    @NotNull(message = "Completá el precio") @DecimalMin(value = "0.01", message = "Tiene que ser mayor a 0") @Digits(integer = 8, fraction = 2, message = "Puede tener hasta 2 decimales") BigDecimal precioBase,
    @NotNull(message = "Elegí el formato") FormatoFuncion formato,
    @NotNull(message = "Elegí el idioma") Idioma idioma
) {
}
