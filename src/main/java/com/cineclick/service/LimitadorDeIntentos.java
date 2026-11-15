package com.cineclick.service;

import com.cineclick.exception.DemasiadasSolicitudesException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class LimitadorDeIntentos {

    private final Map<String, Deque<Instant>> intentos = new ConcurrentHashMap<>();
    private final Clock reloj;

    public static String clave(String accion, String email, HttpServletRequest request) {
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return accion + "|" + emailNormalizado + "|" + request.getRemoteAddr();
    }

    public void verificar(String clave, int maximo, Duration ventana) {
        Deque<Instant> deLaClave = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (deLaClave) {
            rechazarSiSuperaElLimite(deLaClave, reloj.instant(), maximo, ventana);
        }
    }

    public void registrar(String clave) {
        Deque<Instant> deLaClave = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (deLaClave) {
            deLaClave.addLast(reloj.instant());
        }
    }

    public void verificarYRegistrar(String clave, int maximo, Duration ventana) {
        Deque<Instant> deLaClave = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (deLaClave) {
            Instant ahora = reloj.instant();
            rechazarSiSuperaElLimite(deLaClave, ahora, maximo, ventana);
            deLaClave.addLast(ahora);
        }
    }

    public void limpiar(String clave) {
        intentos.remove(clave);
    }

    private static void rechazarSiSuperaElLimite(Deque<Instant> deLaClave, Instant ahora, int maximo, Duration ventana) {
        while (!deLaClave.isEmpty() && !deLaClave.peekFirst().plus(ventana).isAfter(ahora)) {
            deLaClave.pollFirst();
        }
        if (deLaClave.size() >= maximo) {
            Duration espera = Duration.between(ahora, deLaClave.peekFirst().plus(ventana));
            long minutos = Math.max(1, (espera.toSeconds() + 59) / 60);
            throw new DemasiadasSolicitudesException(
                "Demasiados intentos. Probá de nuevo en " + minutos + (minutos == 1 ? " minuto" : " minutos"));
        }
    }
}
