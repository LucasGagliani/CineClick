package com.cineclick.service;

import com.cineclick.dto.CompraAdminDTO;
import com.cineclick.dto.DetalleCompraAdminDTO;
import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.PaginaDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.Compra;
import com.cineclick.model.EstadoCompra;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.ConsultaSoporteRepository;
import com.cineclick.repository.SolicitudArrepentimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CompraAdminService {

    private static final int TAMANO_MAXIMO = 100;

    private final CompraRepository compraRepository;
    private final SolicitudArrepentimientoRepository arrepentimientoRepository;
    private final ConsultaSoporteRepository consultaSoporteRepository;

    @Transactional(readOnly = true)
    public PaginaDTO<CompraAdminDTO> buscar(String codigo, String email, Long cineId, EstadoCompra estado,
                                            LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        String limpio = codigo == null ? "" : codigo.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        Long compraId = limpio.matches("\\d{1,18}") ? Long.valueOf(limpio) : null;
        String codigoEntrada = !limpio.isEmpty() && compraId == null ? limpio : null;
        String filtroEmail = email == null || email.isBlank() ? null : "%" + email.trim().toLowerCase(Locale.ROOT) + "%";
        PageRequest orden = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), TAMANO_MAXIMO),
            Sort.by(Sort.Direction.DESC, "fechaCompra").and(Sort.by(Sort.Direction.DESC, "id")));
        return PaginaDTO.de(
            compraRepository.buscarParaAdmin(
                compraId, codigoEntrada, filtroEmail, cineId, estado,
                desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(),
                orden
            ),
            CompraAdminService::toFila
        );
    }

    @Transactional(readOnly = true)
    public DetalleCompraAdminDTO detalle(Long id) {
        Compra compra = compraRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada con id " + id));
        return new DetalleCompraAdminDTO(
            DtoMapper.toCompraResponse(compra),
            compra.getCliente().getUsuario().getEmail(),
            compra.getPromocion() == null ? null : DtoMapper.toPromocionResponse(compra.getPromocion()),
            arrepentimientoRepository.findByCompraId(id).map(ArrepentimientoService::toResponse).orElse(null),
            consultaSoporteRepository.findByCompraIdOrderByFechaDesc(id).stream().map(SoporteService::toResponse).toList()
        );
    }

    private static CompraAdminDTO toFila(Compra compra) {
        return new CompraAdminDTO(
            compra.getId(),
            compra.getFechaCompra(),
            compra.getCliente().nombreCompleto(),
            compra.getCliente().getUsuario().getEmail(),
            compra.getFuncion().getPelicula().getTitulo(),
            compra.getFuncion().getSala().getCine().getNombre(),
            compra.getFuncion().getSala().getNombre(),
            compra.getFuncion().getFechaHoraInicio(),
            compra.getEntradas().size(),
            compra.getTotal(),
            compra.getEstado(),
            compra.getPromocion() == null ? null : compra.getPromocion().getCodigo()
        );
    }
}
