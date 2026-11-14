package com.cineclick.service;

import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.PagoRequestDTO;
import com.cineclick.dto.PagoResponseDTO;
import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.exception.PromocionRechazadaException;
import com.cineclick.exception.RecursoDuplicadoException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.Compra;
import com.cineclick.model.EstadoPago;
import com.cineclick.model.MetodoPago;
import com.cineclick.model.Pago;
import com.cineclick.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoService {

    public static final String ULTIMOS4_TARJETA_RECHAZADA = "0002";

    private final PagoRepository pagoRepository;
    private final CompraService compraService;
    private final PromocionService promocionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(noRollbackFor = PromocionRechazadaException.class)
    public PagoResponseDTO registrarPago(Long compraId, PagoRequestDTO dto) {
        Compra compra = compraService.buscarEntidad(compraId);
        Optional<Pago> existente = pagoRepository.findByCompraId(compraId);

        if (existente.isPresent() && existente.get().getEstado() == EstadoPago.APROBADO) {
            return DtoMapper.toPagoResponse(existente.get());
        }
        compraService.verificarReservaActiva(compra);

        boolean sinCargo = compra.getTotal().signum() == 0;
        if (!sinCargo) {
            validarDatosDeTarjeta(dto);
        }

        Pago pago = existente.orElseGet(Pago::new);
        pago.setCompra(compra);
        pago.setMonto(compra.getTotal());
        pago.setMetodoPago(sinCargo ? MetodoPago.SIN_CARGO : dto.metodoPago());
        pago.setMarcaTarjeta(sinCargo ? null : dto.marca());
        pago.setUltimos4(sinCargo ? null : dto.ultimos4());

        if (!sinCargo && ULTIMOS4_TARJETA_RECHAZADA.equals(dto.ultimos4())) {
            pago.rechazar();
        } else {
            confirmarPromocion(compra);
            pago.confirmar("TX-" + UUID.randomUUID());
            compra.confirmar();
        }
        compra.setPago(pago);
        try {
            Pago guardado = pagoRepository.saveAndFlush(pago);
            if (guardado.getEstado() == EstadoPago.APROBADO) {
                eventPublisher.publishEvent(new PagoAprobadoEvent(guardado.getId()));
            }
            return DtoMapper.toPagoResponse(guardado);
        } catch (DataIntegrityViolationException ex) {
            throw new RecursoDuplicadoException("La compra ya tiene un pago en proceso");
        }
    }

    private void confirmarPromocion(Compra compra) {
        if (compra.getPromocion() == null) {
            return;
        }
        try {
            promocionService.confirmarUso(compra, LocalDateTime.now());
        } catch (PromocionRechazadaException e) {
            compra.setPromocion(null);
            compra.calcularTotal();
            throw e;
        }
    }

    private static void validarDatosDeTarjeta(PagoRequestDTO dto) {
        if (dto.metodoPago() == null || dto.metodoPago() == MetodoPago.SIN_CARGO) {
            throw new CampoInvalidoException("metodoPago", "Elegí el medio de pago");
        }
        if (dto.marca() == null || dto.marca().isBlank()) {
            throw new CampoInvalidoException("marca", "Falta la marca de la tarjeta");
        }
        if (dto.ultimos4() == null) {
            throw new CampoInvalidoException("ultimos4", "Faltan los ultimos 4 digitos de la tarjeta");
        }
    }

    @Transactional(readOnly = true)
    public PagoResponseDTO buscarPorId(Long id) {
        Pago pago = pagoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado con id " + id));
        return DtoMapper.toPagoResponse(pago);
    }
}
