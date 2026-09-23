package com.cineclick.service;

import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.PromocionRequestDTO;
import com.cineclick.dto.PromocionResponseDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Promocion;
import com.cineclick.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromocionService {

    private final PromocionRepository promocionRepository;

    @Transactional(readOnly = true)
    public List<PromocionResponseDTO> listarActivas() {
        return promocionRepository.findByActivaTrue().stream().map(DtoMapper::toPromocionResponse).toList();
    }

    @Transactional
    public PromocionResponseDTO crear(PromocionRequestDTO dto) {
        validarFechas(dto.fechaInicio(), dto.fechaFin());
        Promocion promocion = new Promocion();
        copiarDatos(dto, promocion);
        return DtoMapper.toPromocionResponse(promocionRepository.save(promocion));
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

    private void copiarDatos(PromocionRequestDTO dto, Promocion promocion) {
        promocion.setCodigo(dto.codigo());
        promocion.setDescripcion(dto.descripcion());
        promocion.setPorcentajeDescuento(dto.porcentajeDescuento());
        promocion.setFechaInicio(dto.fechaInicio());
        promocion.setFechaFin(dto.fechaFin());
        promocion.setActiva(dto.activa());
    }

    private void validarFechas(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw new ReglaNegocioException("La fecha de fin de una promocion no puede ser anterior al inicio");
        }
    }
}
