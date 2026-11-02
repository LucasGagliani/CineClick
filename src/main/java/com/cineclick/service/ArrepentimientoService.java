package com.cineclick.service;

import com.cineclick.dto.ArrepentimientoRequestDTO;
import com.cineclick.dto.ArrepentimientoResponseDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Compra;
import com.cineclick.model.EstadoArrepentimiento;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.SolicitudArrepentimiento;
import com.cineclick.repository.SolicitudArrepentimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArrepentimientoService {

    private final SolicitudArrepentimientoRepository solicitudRepository;
    private final BusquedaCompraService busquedaCompraService;
    private final PromocionService promocionService;

    @Transactional
    public ArrepentimientoResponseDTO solicitar(ArrepentimientoRequestDTO dto) {
        Compra compra = busquedaCompraService.buscar(dto.email(), dto.codigo());
        Optional<SolicitudArrepentimiento> existente = solicitudRepository.findByCompraId(compra.getId());
        if (existente.isPresent()) {
            return toResponse(existente.get());
        }
        if (compra.getEstado() != EstadoCompra.CONFIRMADA) {
            throw new ReglaNegocioException("Esa compra no está confirmada: no hay entradas para anular");
        }
        LocalDateTime ahora = LocalDateTime.now();
        BusquedaCompraService.Elegibilidad elegibilidad = busquedaCompraService.elegibilidad(compra, ahora);

        SolicitudArrepentimiento solicitud = new SolicitudArrepentimiento();
        solicitud.setCodigo(GeneradorCodigos.nuevo("ARR-", solicitudRepository::existsByCodigo));
        solicitud.setCompra(compra);
        solicitud.setFecha(ahora);
        solicitud.setMotivo(dto.motivo());
        solicitud.setNombre(Normalizador.texto(dto.nombre()));
        solicitud.setEmail(dto.email().trim().toLowerCase());
        if (elegibilidad.elegible()) {
            anular(solicitud, ahora);
        } else {
            solicitud.setEstado(EstadoArrepentimiento.FUERA_DE_PLAZO);
            solicitud.setMotivoFueraDePlazo(elegibilidad.motivo());
        }
        return toResponse(solicitudRepository.saveAndFlush(solicitud));
    }

    @Transactional(readOnly = true)
    public List<ArrepentimientoResponseDTO> listar() {
        return solicitudRepository.findAllByOrderByFechaDesc().stream().map(ArrepentimientoService::toResponse).toList();
    }

    @Transactional
    public ArrepentimientoResponseDTO aprobar(Long id) {
        SolicitudArrepentimiento solicitud = pendiente(id);
        anular(solicitud, LocalDateTime.now());
        return toResponse(solicitud);
    }

    @Transactional
    public ArrepentimientoResponseDTO rechazar(Long id) {
        SolicitudArrepentimiento solicitud = pendiente(id);
        solicitud.setEstado(EstadoArrepentimiento.RECHAZADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        return toResponse(solicitud);
    }

    private void anular(SolicitudArrepentimiento solicitud, LocalDateTime ahora) {
        Compra compra = solicitud.getCompra();
        compra.arrepentir();
        if (compra.getPromocion() != null) {
            promocionService.devolverUso(compra.getPromocion());
        }
        solicitud.setEstado(EstadoArrepentimiento.APROBADA);
        solicitud.setMontoReembolso(compra.getTotal());
        solicitud.setFechaReembolso(ahora);
        solicitud.setFechaResolucion(ahora);
    }

    private SolicitudArrepentimiento pendiente(Long id) {
        SolicitudArrepentimiento solicitud = solicitudRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada con id " + id));
        if (solicitud.getEstado() != EstadoArrepentimiento.FUERA_DE_PLAZO) {
            throw new ReglaNegocioException("Solo se pueden resolver las solicitudes fuera de plazo");
        }
        return solicitud;
    }

    public static ArrepentimientoResponseDTO toResponse(SolicitudArrepentimiento solicitud) {
        return new ArrepentimientoResponseDTO(
            solicitud.getId(),
            solicitud.getCodigo(),
            solicitud.getEstado(),
            solicitud.getFecha(),
            solicitud.getCompra().getId(),
            solicitud.getCompra().getFuncion().getPelicula().getTitulo(),
            solicitud.getNombre(),
            solicitud.getEmail(),
            solicitud.getMotivo(),
            solicitud.getMotivoFueraDePlazo(),
            solicitud.getMontoReembolso(),
            solicitud.getFechaReembolso()
        );
    }
}
