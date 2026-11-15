package com.cineclick.service;

import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.TokenRecuperacion;
import com.cineclick.model.Usuario;
import com.cineclick.repository.TokenRecuperacionRepository;
import com.cineclick.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecuperacionPasswordService {

    public static final int MAXIMO_PEDIDOS = 3;
    public static final Duration VENTANA_PEDIDOS = Duration.ofMinutes(15);
    public static final int MINUTOS_VIGENCIA = 30;
    public static final String ENLACE_INVALIDO = "Este enlace venció o ya se usó";

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final LimitadorDeIntentos limitador;
    private final Clock reloj;

    @Value("${cineclick.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public void solicitar(String email, HttpServletRequest request) {
        limitador.verificarYRegistrar(LimitadorDeIntentos.clave("recuperar", email, request), MAXIMO_PEDIDOS, VENTANA_PEDIDOS);
        usuarioRepository.findByEmailIgnoreCase(email.trim())
            .filter(Usuario::isActivo)
            .ifPresent(this::generarYEnviar);
    }

    @Transactional(readOnly = true)
    public void validar(String token) {
        buscarVigente(token);
    }

    @Transactional
    public String restablecer(String token, String nuevaPassword) {
        TokenRecuperacion registro = buscarVigente(token);
        Usuario usuario = registro.getUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setContrasenaModificadaEn(ahora);
        registro.setUsadoEn(ahora);
        return usuario.getEmail();
    }

    private void generarYEnviar(Usuario usuario) {
        String token = TokensSeguros.generar();

        tokenRepository.deleteByUsuarioId(usuario.getId());
        tokenRepository.flush();
        TokenRecuperacion registro = new TokenRecuperacion();
        registro.setUsuario(usuario);
        registro.setTokenHash(TokensSeguros.hash(token));
        registro.setExpiraEn(LocalDateTime.now(reloj).plusMinutes(MINUTOS_VIGENCIA));
        tokenRepository.save(registro);

        String enlace = frontendUrl.replaceAll("/+$", "") + "/restablecer?token=" + token;
        try {
            emailService.enviar(usuario.getEmail(), "Recuperar contraseña - CineClick",
                "Recibimos un pedido para cambiar tu contraseña. Abrí este enlace dentro de los próximos "
                    + MINUTOS_VIGENCIA + " minutos:\n\n" + enlace
                    + "\n\nSi no pediste el cambio, ignorá este mensaje: tu contraseña sigue siendo la misma.");
        } catch (Exception e) {
            log.error("No se pudo enviar el enlace de recuperación para el usuario {}", usuario.getId(), e);
        }
    }

    private TokenRecuperacion buscarVigente(String token) {
        if (token == null || token.isBlank()) {
            throw new RecursoNoEncontradoException(ENLACE_INVALIDO);
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return tokenRepository.findByTokenHash(TokensSeguros.hash(token))
            .filter(t -> t.vigenteEn(ahora))
            .orElseThrow(() -> new RecursoNoEncontradoException(ENLACE_INVALIDO));
    }
}
