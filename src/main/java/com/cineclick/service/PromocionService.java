package com.cineclick.service;

import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.PromocionRequestDTO;
import com.cineclick.dto.PromocionResponseDTO;
import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.exception.RecursoEnUsoException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Promocion;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PromocionService {

    private final PromocionRepository promocionRepository;
    private final CompraRepository compraRepository;

    @Transactional(readOnly = true)
    public List<PromocionResponseDTO> listarTodas() {
        return promocionRepository.findAllByOrderByFechaFinDesc().stream().map(DtoMapper::toPromocionResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PromocionResponseDTO> listarVigentes() {
        LocalDate hoy = LocalDate.now();
        return promocionRepository.findByActivaTrue().stream()
            .filter(promocion -> promocion.estaVigente(hoy))
            .sorted(Comparator.comparing(Promocion::getFechaFin))
            .map(DtoMapper::toPromocionResponse)
            .toList();
    }

    @Transactional
    public PromocionResponseDTO crear(PromocionRequestDTO dto) {
        Promocion promocion = new Promocion();
        copiarDatos(dto, promocion);
        return DtoMapper.toPromocionResponse(promocionRepository.save(promocion));
    }

    @Transactional
    public PromocionResponseDTO actualizar(Long id, PromocionRequestDTO dto) {
        Promocion promocion = buscarEntidad(id);
        copiarDatos(dto, promocion);
        return DtoMapper.toPromocionResponse(promocionRepository.save(promocion));
    }

    @Transactional
    public PromocionResponseDTO cambiarEstado(Long id, boolean activa) {
        Promocion promocion = buscarEntidad(id);
        promocion.setActiva(activa);
        return DtoMapper.toPromocionResponse(promocionRepository.save(promocion));
    }

    @Transactional
    public void eliminar(Long id) {
        Promocion promocion = buscarEntidad(id);
        if (compraRepository.existsByPromocionId(id)) {
            throw new RecursoEnUsoException("La promocion tiene compras asociadas", List.of("compras"));
        }
        promocionRepository.delete(promocion);
    }

    @Transactional(readOnly = true)
    public Promocion buscarPorCodigo(String codigo) {
        return promocionRepository.findByCodigoIgnoreCase(codigo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Promocion no encontrada con codigo " + codigo));
    }

    public Promocion validarPromocionParaCompra(String codigo) {
        Promocion promocion = buscarPorCodigo(codigo);
        if (!promocion.estaVigente(LocalDate.now())) {
            throw new ReglaNegocioException("La promocion no esta vigente");
        }
        return promocion;
    }

    private Promocion buscarEntidad(Long id) {
        return promocionRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Promocion no encontrada con id " + id));
    }

    private void copiarDatos(PromocionRequestDTO dto, Promocion promocion) {
        if (dto.fechaFin().isBefore(dto.fechaInicio())) {
            throw new CampoInvalidoException("fechaFin", "No puede ser anterior a la fecha de inicio");
        }
        String codigo = dto.codigo().trim().toUpperCase(Locale.ROOT);
        promocionRepository.findByCodigoIgnoreCase(codigo)
            .filter(otra -> !Objects.equals(otra.getId(), promocion.getId()))
            .ifPresent(otra -> {
                throw new CampoInvalidoException("codigo", "Ya existe una promoción con ese código");
            });
        promocion.setCodigo(codigo);
        promocion.setTitulo(Normalizador.texto(dto.titulo()));
        promocion.setDescripcion(Normalizador.texto(dto.descripcion()));
        promocion.setPorcentajeDescuento(dto.porcentajeDescuento());
        promocion.setFechaInicio(dto.fechaInicio());
        promocion.setFechaFin(dto.fechaFin());
        promocion.setActiva(dto.activa());
    }
}
