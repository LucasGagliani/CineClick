package com.cineclick.service;

import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.PagoRequestDTO;
import com.cineclick.dto.PagoResponseDTO;
import com.cineclick.exception.RecursoDuplicadoException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.Compra;
import com.cineclick.model.EstadoPago;
import com.cineclick.model.Pago;
import com.cineclick.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoService {

    public static final String ULTIMOS4_TARJETA_RECHAZADA = "0002";

    private final PagoRepository pagoRepository;
    private final CompraService compraService;

    @Transactional
    public PagoResponseDTO registrarPago(Long compraId, PagoRequestDTO dto) {
        Compra compra = compraService.buscarEntidad(compraId);
        Optional<Pago> existente = pagoRepository.findByCompraId(compraId);

        if (existente.isPresent() && existente.get().getEstado() == EstadoPago.APROBADO) {
            return DtoMapper.toPagoResponse(existente.get());
        }
        compraService.verificarReservaActiva(compra);

        Pago pago = existente.orElseGet(Pago::new);
        pago.setCompra(compra);
        pago.setMonto(compra.getTotal());
        pago.setMetodoPago(dto.metodoPago());
        pago.setMarcaTarjeta(dto.marca());
        pago.setUltimos4(dto.ultimos4());

        if (ULTIMOS4_TARJETA_RECHAZADA.equals(dto.ultimos4())) {
            pago.rechazar();
        } else {
            pago.confirmar("TX-" + UUID.randomUUID());
            compra.confirmar();
        }
        compra.setPago(pago);
        try {
            return DtoMapper.toPagoResponse(pagoRepository.saveAndFlush(pago));
        } catch (DataIntegrityViolationException ex) {
            throw new RecursoDuplicadoException("La compra ya tiene un pago en proceso");
        }
    }

    @Transactional(readOnly = true)
    public PagoResponseDTO buscarPorId(Long id) {
        Pago pago = pagoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado con id " + id));
        return DtoMapper.toPagoResponse(pago);
    }
}
