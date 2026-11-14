package com.cineclick.service;

import com.cineclick.model.PasswordResetToken;
import com.cineclick.model.Usuario;
import com.cineclick.repository.PasswordResetTokenRepository;
import com.cineclick.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecuperacionPasswordService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${cineclick.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public void solicitar(String email) {
        usuarioRepository.findByEmailIgnoreCase(email.trim()).ifPresent(usuario -> {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            tokenRepository.deleteByUsuarioId(usuario.getId());
            PasswordResetToken registro = new PasswordResetToken();
            registro.setUsuario(usuario);
            registro.setTokenHash(hash(token));
            registro.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            tokenRepository.save(registro);
            String enlace = frontendUrl.replaceAll("/+$", "") + "/restablecer-contrasena?token=" + token;
            try {
                emailService.enviar(usuario.getEmail(), "Recuperar contraseña - CineClick",
                    "Recibimos un pedido para cambiar tu contraseña. Abrí este enlace dentro de los próximos 30 minutos:\n\n"
                    + enlace + "\n\nSi no solicitaste el cambio, ignorá este mensaje.");
            } catch (Exception e) {
                log.error("No se pudo enviar el enlace de recuperación para el usuario {}", usuario.getId(), e);
            }
        });
    }

    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        if (token == null || token.isBlank() || nuevaPassword == null || nuevaPassword.length() < 8) {
            throw new IllegalArgumentException("El enlace no es válido o venció. Solicitá uno nuevo.");
        }
        PasswordResetToken registro = tokenRepository.findByTokenHashAndUsedAtIsNull(hash(token))
            .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
            .orElseThrow(() -> new IllegalArgumentException("El enlace no es válido o venció. Solicitá uno nuevo."));
        Usuario usuario = registro.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        registro.setUsedAt(LocalDateTime.now());
        tokenRepository.save(registro);
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
