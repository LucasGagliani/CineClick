package com.cineclick.dto;

import com.cineclick.model.Provincia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CineRequestDTO(
    @NotBlank(message = "Completá el nombre") @Size(max = 120) String nombre,
    @NotNull(message = "Elegí la provincia") Provincia provincia,
    @NotBlank(message = "Completá la localidad") @Size(max = 80) String localidad,
    @NotBlank(message = "Completá la calle") @Size(max = 120) String calle,
    @NotNull(message = "Completá la altura") @Min(value = 1, message = "Tiene que ser un número mayor a 0") @Max(value = 99999, message = "Tiene que tener hasta 5 cifras") Integer altura,
    @Size(max = 40) String telefono,
    @Size(max = 300) @Pattern(regexp = "^(https?://|/).*", message = "Tiene que empezar con http://, https:// o /") String urlImagen
) {
}
