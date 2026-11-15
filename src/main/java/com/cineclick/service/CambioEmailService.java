package com.cineclick.service;

import com.cineclick.dto.CuentaResponseDTO;
import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.exception.RecursoDuplicadoException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.TokenCambioEmail;
import com.cineclick.model.Usuario;
import com.cineclick.repository.TokenCambioEmailRepository;
import com.cineclick.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CambioEmailService {

    public static final int MINUTOS_VIGENCIA = 30;
    public static final int MAXIMO_ENVIOS = 3;
    public static final Duration VENTANA_ENVIOS = Duration.ofMinutes(15);
    public static final String ENLACE_INVALIDO = "Este enlace venció o ya se usó";

    private final SesionUsuario sesion;
    private final VerificadorDeContrasena verificador;
    private final CuentaService cuentaService;
    private final UsuarioRepository usuarioRepository;
    private final TokenCambioEmailRepository tokenRepository;
    private final LimitadorDeIntentos limitador;
    private final EmailService emailService;
    private final Clock reloj;

    @Value("${cineclick.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public CuentaResponseDTO iniciar(String email, String contrasena, HttpServletRequest request) {
        Usuario usuario = sesion.actual(request);
        String nuevo = email.trim().toLowerCase(Locale.ROOT);
        if (nuevo.equalsIgnoreCase(usuario.getEmail())) {
            throw new CampoInvalidoException("email", "Ese ya es el email de tu cuenta");
        }
        verificador.verificar(usuario, contrasena, "contrasena", request);
        if (usuarioRepository.existsByEmailIgnoreCase(nuevo)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta con ese email");
        }
        limitarEnvios(usuario, request);
        generarYEnviar(usuario, nuevo);
        return cuentaService.armar(usuario);
    }

    @Transactional
    public CuentaResponseDTO reenviar(HttpServletRequest request) {
        Usuario usuario = sesion.actual(request);
        TokenCambioEmail pendiente = pendienteDe(usuario);
        limitarEnvios(usuario, request);
        generarYEnviar(usuario, pendiente.getEmailNuevo());
        return cuentaService.armar(usuario);
    }

    @Transactional
    public CuentaResponseDTO cancelar(HttpServletRequest request) {
        Usuario usuario = sesion.actual(request);
        tokenRepository.deleteByUsuarioId(usuario.getId());
        tokenRepository.flush();
        return cuentaService.armar(usuario);
    }

    @Transactional
    public CuentaResponseDTO confirmar(String token, HttpServletRequest request) {
        Usuario usuario = sesion.actual(request);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        TokenCambioEmail registro = tokenRepository.findByTokenHash(TokensSeguros.hash(token == null ? "" : token))
            .filter(t -> t.vigenteEn(ahora))
            .filter(t -> t.getUsuario().getId().equals(usuario.getId()))
            .orElseThrow(() -> new RecursoNoEncontradoException(ENLACE_INVALIDO));
        String nuevo = registro.getEmailNuevo();
        if (usuarioRepository.existsByEmailIgnoreCase(nuevo)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta con ese email");
        }
        String anterior = usuario.getEmail();
        usuario.setEmail(nuevo);
        registro.setUsadoEn(ahora);
        enviar(anterior, "El email de tu cuenta cambió - CineClick",
            "El email de tu cuenta de CineClick cambió a " + nuevo + ". Desde ahora vas a ingresar con ese email.\n\n"
                + "Si no hiciste este cambio, escribinos desde " + base() + "/soporte.");
        return cuentaService.armar(usuario);
    }

    private TokenCambioEmail pendienteDe(Usuario usuario) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return tokenRepository.findByUsuarioIdAndUsadoEnIsNullOrderByIdDesc(usuario.getId()).stream()
            .filter(t -> t.vigenteEn(ahora))
            .findFirst()
            .orElseThrow(() -> new RecursoNoEncontradoException("No hay un cambio de email pendiente"));
    }

    private void limitarEnvios(Usuario usuario, HttpServletRequest request) {
        limitador.verificarYRegistrar(LimitadorDeIntentos.clave("cambio-email", usuario.getEmail(), request), MAXIMO_ENVIOS, VENTANA_ENVIOS);
    }

    private void generarYEnviar(Usuario usuario, String emailNuevo) {
        String token = TokensSeguros.generar();
        tokenRepository.deleteByUsuarioId(usuario.getId());
        tokenRepository.flush();
        TokenCambioEmail registro = new TokenCambioEmail();
        registro.setUsuario(usuario);
        registro.setEmailNuevo(emailNuevo);
        registro.setTokenHash(TokensSeguros.hash(token));
        registro.setExpiraEn(LocalDateTime.now(reloj).plusMinutes(MINUTOS_VIGENCIA));
        tokenRepository.save(registro);

        String enlace = base() + "/cuenta/confirmar-email?token=" + token;
        enviar(emailNuevo, "Confirmá tu nuevo email - CineClick",
            "Pediste usar este email en tu cuenta de CineClick. Para confirmarlo, abrí este enlace dentro de los próximos "
                + MINUTOS_VIGENCIA + " minutos (con la sesión iniciada):\n\n" + enlace
                + "\n\nHasta que lo confirmes, tu cuenta sigue usando el email anterior. Si no lo pediste, ignorá este mensaje.");
    }

    private void enviar(String destino, String asunto, String texto) {
        try {
            emailService.enviar(destino, asunto, texto);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo \"{}\"", asunto, e);
        }
    }

    private String base() {
        return frontendUrl.replaceAll("/+$", "");
    }
}
