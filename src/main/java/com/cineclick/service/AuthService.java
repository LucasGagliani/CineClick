package com.cineclick.service;

import com.cineclick.dto.LoginRequestDTO;
import com.cineclick.dto.LoginResponseDTO;
import com.cineclick.exception.CredencialesInvalidasException;
import com.cineclick.model.Usuario;
import com.cineclick.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(dto.email())
            .orElseThrow(() -> new CredencialesInvalidasException("Email o password incorrectos"));
        if (!usuario.isActivo() || !usuario.getPassword().equals(dto.password())) {
            throw new CredencialesInvalidasException("Email o password incorrectos");
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
