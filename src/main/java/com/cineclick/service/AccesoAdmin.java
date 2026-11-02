package com.cineclick.service;

import com.cineclick.exception.AccesoDenegadoException;
import com.cineclick.model.RolUsuario;
import com.cineclick.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccesoAdmin {

    private final UsuarioRepository usuarioRepository;

    public void verificar(Long usuarioId) {
        boolean esAdmin = usuarioId != null && usuarioRepository.findById(usuarioId)
            .map(usuario -> usuario.isActivo() && usuario.getRol() == RolUsuario.ADMIN)
            .orElse(false);
        if (!esAdmin) {
            throw new AccesoDenegadoException("Solo un administrador puede ver esta informacion");
        }
    }
}
