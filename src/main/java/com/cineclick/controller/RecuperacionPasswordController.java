package com.cineclick.controller;

import com.cineclick.service.RecuperacionPasswordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RecuperacionPasswordController {
    private final RecuperacionPasswordService service;

    public record Solicitud(@NotBlank @Email String email) {}
    public record Cambio(@NotBlank String token, @NotBlank @Size(min = 8, max = 100) String nuevaPassword) {}

    @PostMapping("/recuperar-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mensaje solicitar(@Valid @RequestBody Solicitud solicitud) {
        service.solicitar(solicitud.email());
        return new Mensaje("Si el correo está registrado, recibirás un enlace para cambiar tu contraseña.");
    }

    @PostMapping("/restablecer-password")
    public Mensaje restablecer(@Valid @RequestBody Cambio cambio) {
        service.restablecer(cambio.token(), cambio.nuevaPassword());
        return new Mensaje("La contraseña se actualizó. Ya podés iniciar sesión.");
    }

    public record Mensaje(String mensaje) {}
}
