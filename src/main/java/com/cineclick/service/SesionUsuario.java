package com.cineclick.service;

import com.cineclick.config.InterceptorAdmin;
import com.cineclick.exception.CredencialesInvalidasException;
import com.cineclick.model.Usuario;
import com.cineclick.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SesionUsuario {

    private final UsuarioRepository usuarioRepository;

    public Usuario actual(HttpServletRequest request) {
        Long id = InterceptorAdmin.usuarioDe(request);
        if (id == null) {
            throw new CredencialesInvalidasException("Iniciá sesión para ver tu cuenta");
        }
        return usuarioRepository.findById(id)
            .filter(Usuario::isActivo)
            .orElseThrow(() -> new CredencialesInvalidasException("Iniciá sesión para ver tu cuenta"));
    }
}
