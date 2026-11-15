package com.cineclick.dto;

import com.cineclick.model.GeneroCliente;
import com.cineclick.model.RolUsuario;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CuentaResponseDTO(
    Long usuarioId,
    Long clienteId,
    String nombre,
    String apellido,
    String nombreCompleto,
    String email,
    RolUsuario rol,
    LocalDateTime clienteDesde,
    String telefono,
    LocalDate fechaNacimiento,
    GeneroCliente genero,
    Long cinePreferidoId,
    String cinePreferidoNombre,
    LocalDateTime contrasenaModificadaEn,
    boolean recibirNovedades,
    String emailPendiente,
    LocalDateTime emailPendienteExpiraEn
) {
}
