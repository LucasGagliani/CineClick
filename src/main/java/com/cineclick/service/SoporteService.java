package com.cineclick.service;

import com.cineclick.dto.SoporteRequestDTO;
import com.cineclick.dto.SoporteResponseDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.Compra;
import com.cineclick.model.ConsultaSoporte;
import com.cineclick.model.EstadoConsulta;
import com.cineclick.repository.ConsultaSoporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SoporteService {

    private final ConsultaSoporteRepository consultaRepository;
    private final BusquedaCompraService busquedaCompraService;

    @Transactional
    public SoporteResponseDTO crear(SoporteRequestDTO dto) {
        String clave = Normalizador.texto(dto.claveIdempotencia());
        if (clave != null) {
            Optional<ConsultaSoporte> existente = consultaRepository.findByClaveIdempotencia(clave);
            if (existente.isPresent()) {
                return toResponse(existente.get());
            }
        }
        Compra compra = Normalizador.texto(dto.codigo()) == null ? null : busquedaCompraService.buscar(dto.email(), dto.codigo());
        ConsultaSoporte consulta = new ConsultaSoporte();
        consulta.setNumero(GeneradorCodigos.nuevo("SOP-", consultaRepository::existsByNumero));
        consulta.setNombre(Normalizador.texto(dto.nombre()));
        consulta.setEmail(dto.email().trim().toLowerCase());
        consulta.setMotivo(dto.motivo());
        consulta.setCompra(compra);
        consulta.setMensaje(Normalizador.texto(dto.mensaje()));
        consulta.setFecha(LocalDateTime.now());
        consulta.setClaveIdempotencia(clave);
        return toResponse(consultaRepository.saveAndFlush(consulta));
    }

    @Transactional(readOnly = true)
    public List<SoporteResponseDTO> listar(EstadoConsulta estado) {
        List<ConsultaSoporte> consultas = estado == null
            ? consultaRepository.findAllByOrderByFechaDesc()
            : consultaRepository.findByEstadoOrderByFechaDesc(estado);
        return consultas.stream().map(SoporteService::toResponse).toList();
    }

    @Transactional
    public SoporteResponseDTO resolver(Long id) {
        ConsultaSoporte consulta = consultaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Consulta no encontrada con id " + id));
        consulta.setEstado(EstadoConsulta.RESUELTA);
        consulta.setFechaResolucion(LocalDateTime.now());
        return toResponse(consulta);
    }

    private static SoporteResponseDTO toResponse(ConsultaSoporte consulta) {
        return new SoporteResponseDTO(
            consulta.getId(),
            consulta.getNumero(),
            consulta.getNombre(),
            consulta.getEmail(),
            consulta.getMotivo(),
            consulta.getCompra() == null ? null : consulta.getCompra().getId(),
            consulta.getMensaje(),
            consulta.getFecha(),
            consulta.getEstado()
        );
    }
}
