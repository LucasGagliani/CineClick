package com.cineclick.service;

import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerificadorDeContrasena {

    private final PasswordEncoder passwordEncoder;
    private final LimitadorDeIntentos limitador;

    public void verificar(Usuario usuario, String contrasena, String campo, HttpServletRequest request) {
        String clave = LimitadorDeIntentos.clave("login", usuario.getEmail(), request);
        limitador.verificar(clave, AuthService.MAXIMO_INTENTOS_FALLIDOS, AuthService.VENTANA_INTENTOS);
        if (contrasena == null || !passwordEncoder.matches(contrasena, usuario.getPassword())) {
            limitador.registrar(clave);
            throw new CampoInvalidoException(campo, "La contraseña no es correcta");
        }
    }
}
