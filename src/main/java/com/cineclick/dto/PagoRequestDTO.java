package com.cineclick.dto;

import com.cineclick.model.MetodoPago;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PagoRequestDTO(
    MetodoPago metodoPago,
    @Size(max = 20) String marca,
    @Pattern(regexp = "[0-9]{4}", message = "tienen que ser los ultimos 4 digitos de la tarjeta") String ultimos4,
    @NotNull @AssertTrue(message = "hay que aceptar los terminos y condiciones") Boolean aceptaTerminos
) {
}
