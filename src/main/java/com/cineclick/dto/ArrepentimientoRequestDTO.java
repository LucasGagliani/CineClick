package com.cineclick.dto;

import com.cineclick.model.MotivoArrepentimiento;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ArrepentimientoRequestDTO(
    @NotBlank(message = "Completá el email") @Email(message = "Revisá el email") @Size(max = 120) String email,
    @NotBlank(message = "Completá el número de compra o el código de una entrada") @Size(max = 30) String codigo,
    @NotBlank(message = "Completá tu nombre y apellido") @Size(max = 120) String nombre,
    MotivoArrepentimiento motivo,
    @NotNull @AssertTrue(message = "Tenés que aceptar que se anulan todas las entradas de la compra") Boolean confirmaAnulacion
) {
}
