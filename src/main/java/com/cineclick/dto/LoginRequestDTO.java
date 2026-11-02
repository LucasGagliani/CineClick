package com.cineclick.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 72) String password
) {
}
