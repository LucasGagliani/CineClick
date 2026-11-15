package com.cineclick.service;

import com.cineclick.dto.LoginRequestDTO;
import com.cineclick.dto.LoginResponseDTO;
import com.cineclick.exception.CredencialesInvalidasException;
import com.cineclick.model.Cliente;
import com.cineclick.model.Usuario;
import com.cineclick.repository.ClienteRepository;
import com.cineclick.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final int MAXIMO_INTENTOS_FALLIDOS = 5;
    public static final Duration VENTANA_INTENTOS = Duration.ofMinutes(15);
    public static final String CREDENCIALES_INCORRECTAS = "El email o la contraseña no son correctos";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final LimitadorDeIntentos limitador;

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto, HttpServletRequest request) {
        String clave = LimitadorDeIntentos.clave("login", dto.email(), request);
        limitador.verificar(clave, MAXIMO_INTENTOS_FALLIDOS, VENTANA_INTENTOS);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(dto.email().trim())
            .filter(Usuario::isActivo)
            .filter(u -> passwordEncoder.matches(dto.password(), u.getPassword()))
            .orElse(null);
        if (usuario == null) {
            limitador.registrar(clave);
            throw new CredencialesInvalidasException(CREDENCIALES_INCORRECTAS);
        }
        limitador.limpiar(clave);
        Long clienteId = clienteRepository.findByUsuarioEmailIgnoreCase(usuario.getEmail())
            .map(Cliente::getId)
            .orElse(null);
        return new LoginResponseDTO(
            usuario.getId(),
            clienteId,
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.nombreCompleto(),
            usuario.getEmail(),
            usuario.getRol(),
            "Login correcto para usuario " + usuario.getRol()
        );
    }
}
