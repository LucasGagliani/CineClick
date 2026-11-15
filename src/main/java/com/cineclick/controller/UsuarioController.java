package com.cineclick.controller;

import com.cineclick.dto.CuentaResponseDTO;
import com.cineclick.dto.DatosPersonalesRequestDTO;
import com.cineclick.dto.ExportacionDatosDTO;
import com.cineclick.service.CambioEmailService;
import com.cineclick.service.CuentaService;
import com.cineclick.service.ExportacionDatosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/usuarios/me")
@RequiredArgsConstructor
public class UsuarioController {

    private final CuentaService cuentaService;
    private final CambioEmailService cambioEmailService;
    private final ExportacionDatosService exportacionService;

    public record CambioEmail(
        @NotBlank(message = "Ingresá el email nuevo") @Email(message = "Revisá el email") @Size(max = 120) String email,
        @NotBlank(message = "Ingresá tu contraseña actual") String contrasena
    ) {}

    public record ConfirmarEmail(@NotBlank String token) {}

    public record CambioContrasena(
        @NotBlank(message = "Ingresá tu contraseña actual") String actual,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "La contraseña tiene que tener al menos una letra y un número")
        String nueva
    ) {}

    public record Comunicaciones(@NotNull Boolean recibirNovedades) {}

    public record ConContrasena(@NotBlank(message = "Ingresá tu contraseña") String contrasena) {}

    @GetMapping
    public CuentaResponseDTO ver(HttpServletRequest request) {
        return cuentaService.ver(request);
    }

    @PatchMapping
    public CuentaResponseDTO actualizarDatos(@Valid @RequestBody DatosPersonalesRequestDTO dto, HttpServletRequest request) {
        return cuentaService.actualizarDatos(dto, request);
    }

    @PostMapping("/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CuentaResponseDTO iniciarCambioEmail(@Valid @RequestBody CambioEmail dto, HttpServletRequest request) {
        return cambioEmailService.iniciar(dto.email(), dto.contrasena(), request);
    }

    @PostMapping("/email/reenviar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CuentaResponseDTO reenviarCambioEmail(HttpServletRequest request) {
        return cambioEmailService.reenviar(request);
    }

    @DeleteMapping("/email")
    public CuentaResponseDTO cancelarCambioEmail(HttpServletRequest request) {
        return cambioEmailService.cancelar(request);
    }

    @PostMapping("/email/confirmar")
    public CuentaResponseDTO confirmarCambioEmail(@Valid @RequestBody ConfirmarEmail dto, HttpServletRequest request) {
        return cambioEmailService.confirmar(dto.token(), request);
    }

    @PutMapping("/contrasena")
    public CuentaResponseDTO cambiarContrasena(@Valid @RequestBody CambioContrasena dto, HttpServletRequest request) {
        return cuentaService.cambiarContrasena(dto.actual(), dto.nueva(), request);
    }

    @PatchMapping("/comunicaciones")
    public CuentaResponseDTO cambiarComunicaciones(@Valid @RequestBody Comunicaciones dto, HttpServletRequest request) {
        return cuentaService.cambiarComunicaciones(dto.recibirNovedades(), request);
    }

    @PostMapping("/exportar")
    public ResponseEntity<ExportacionDatosDTO> exportar(@Valid @RequestBody ConContrasena dto, HttpServletRequest request) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cineclick-mis-datos.json\"")
            .body(exportacionService.exportar(dto.contrasena(), request));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Valid @RequestBody ConContrasena dto, HttpServletRequest request) {
        cuentaService.eliminar(dto.contrasena(), request);
    }
}
