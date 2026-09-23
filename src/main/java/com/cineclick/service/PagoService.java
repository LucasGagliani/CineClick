package com.cineclick.service;

import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.PagoRequestDTO;
import com.cineclick.dto.PagoResponseDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Compra;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.Pago;
import com.cineclick.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final CompraService compraService;

    @Transactional
    public PagoResponseDTO registrarPago(Long compraId, PagoRequestDTO dto) {
        Compra compra = compraService.buscarEntidad(compraId);
        if (compra.getEstado() == EstadoCompra.CANCELADA) {
            throw new ReglaNegocioException("No se puede pagar una compra cancelada");
        }
        if (pagoRepository.findByCompraId(compraId).isPresent()) {
            throw new ReglaNegocioException("La compra ya tiene un pago asociado");
        }
        Pago pago = new Pago();
        pago.setCompra(compra);
        pago.setMonto(compra.getTotal());
        pago.setMetodoPago(dto.metodoPago());

        if (debeRechazarse(dto.datosPagoToken())) {
            pago.rechazar();
        } else {
            pago.confirmar("TX-" + UUID.randomUUID());
            compra.confirmar();
        }
        compra.setPago(pago);
        return DtoMapper.toPagoResponse(pagoRepository.save(pago));
    }

    @Transactional(readOnly = true)
    public PagoResponseDTO buscarPorId(Long id) {
        Pago pago = pagoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado con id " + id));
        return DtoMapper.toPagoResponse(pago);
    }

    private boolean debeRechazarse(String token) {
        return token != null && token.toLowerCase(Locale.ROOT).contains("rechazar");
    }
}
