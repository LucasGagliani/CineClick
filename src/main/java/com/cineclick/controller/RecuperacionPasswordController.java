package com.cineclick.controller;

import com.cineclick.service.RecuperacionPasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RecuperacionPasswordController {

    private final RecuperacionPasswordService service;

    public record Solicitud(@NotBlank @Email @Size(max = 120) String email) {}

    public record Cambio(
        @NotBlank String token,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "La contraseña tiene que tener al menos una letra y un número")
        String password
    ) {}

    public record Mensaje(String mensaje) {}

    public record Restablecida(String mensaje, String email) {}

    @PostMapping("/recuperar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mensaje solicitar(@Valid @RequestBody Solicitud solicitud, HttpServletRequest request) {
        service.solicitar(solicitud.email(), request);
        return new Mensaje("Si el email tiene una cuenta, te enviamos un enlace para cambiar la contraseña.");
    }

    @GetMapping("/recuperar/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validar(@PathVariable String token) {
        service.validar(token);
    }

    @PostMapping("/restablecer")
    public Restablecida restablecer(@Valid @RequestBody Cambio cambio) {
        String email = service.restablecer(cambio.token(), cambio.password());
        return new Restablecida("Listo, ya podés ingresar con tu nueva contraseña", email);
    }
}
