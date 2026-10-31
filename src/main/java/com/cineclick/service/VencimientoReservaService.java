package com.cineclick.service;

import com.cineclick.model.Compra;
import com.cineclick.model.EstadoCompra;
import com.cineclick.repository.CompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VencimientoReservaService {

    private final CompraRepository compraRepository;

    @Transactional
    public int cancelarVencidas(Long funcionId) {
        return cancelar(compraRepository.findByFuncionIdAndEstadoAndFechaCompraBefore(
            funcionId,
            EstadoCompra.PENDIENTE_PAGO,
            limite()
        ));
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void cancelarTodasLasVencidas() {
        cancelar(compraRepository.findByEstadoAndFechaCompraBefore(EstadoCompra.PENDIENTE_PAGO, limite()));
    }

    private LocalDateTime limite() {
        return LocalDateTime.now().minusMinutes(Compra.MINUTOS_RESERVA);
    }

    private int cancelar(List<Compra> vencidas) {
        vencidas.forEach(Compra::cancelar);
        return vencidas.size();
    }
}
