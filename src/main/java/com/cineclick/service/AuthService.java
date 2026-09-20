package com.cineclick.service;

import com.cineclick.dto.LoginRequestDTO;
import com.cineclick.dto.LoginResponseDTO;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Usuario;
import com.cineclick.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(dto.email())
            .orElseThrow(() -> new ReglaNegocioException("Email o password incorrectos"));
        if (!usuario.isActivo() || !usuario.getPassword().equals(dto.password())) {
            throw new ReglaNegocioException("Email o password incorrectos");
        }
        return new LoginResponseDTO(
            usuario.getId(),
            usuario.nombreCompleto(),
            usuario.getEmail(),
            usuario.getRol(),
            "Login correcto para usuario " + usuario.getRol()
        );
    }
}
