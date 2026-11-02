package com.cineclick.service;

import com.cineclick.exception.DemasiadasSolicitudesException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimitadorPorIp {

    private static final long VENTANA_MS = 60_000;

    private final Map<String, Deque<Long>> pedidos = new ConcurrentHashMap<>();

    @Value("${cineclick.limite.pedidos-por-minuto:20}")
    private int pedidosPorMinuto;

    public void verificar(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        String ip = reenviada == null || reenviada.isBlank() ? request.getRemoteAddr() : reenviada.split(",")[0].trim();
        long ahora = System.currentTimeMillis();
        Deque<Long> deIp = pedidos.computeIfAbsent(ip, clave -> new ArrayDeque<>());
        synchronized (deIp) {
            while (!deIp.isEmpty() && ahora - deIp.peekFirst() > VENTANA_MS) {
                deIp.pollFirst();
            }
            if (deIp.size() >= pedidosPorMinuto) {
                throw new DemasiadasSolicitudesException("Hiciste muchos pedidos seguidos. Esperá un minuto y probá de nuevo.");
            }
            deIp.addLast(ahora);
        }
    }
}
