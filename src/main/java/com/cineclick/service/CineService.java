package com.cineclick.service;

import com.cineclick.dto.CineRequestDTO;
import com.cineclick.dto.CineResponseDTO;
import com.cineclick.dto.DtoMapper;
import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.exception.RecursoEnUsoException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.BarriosCaba;
import com.cineclick.model.Cine;
import com.cineclick.model.Provincia;
import com.cineclick.repository.CineRepository;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CineService {

    private final CineRepository cineRepository;
    private final SalaRepository salaRepository;
    private final FuncionRepository funcionRepository;
    private final CompraRepository compraRepository;

    @Transactional(readOnly = true)
    public List<CineResponseDTO> listar(boolean incluirInactivos) {
        List<Cine> cines = incluirInactivos ? cineRepository.findAllByOrderByNombreAsc() : cineRepository.findByActivoTrue();
        return cines.stream().map(DtoMapper::toCineResponse).toList();
    }

    @Transactional(readOnly = true)
    public CineResponseDTO buscarPorId(Long id) {
        return DtoMapper.toCineResponse(buscarEntidad(id));
    }

    @Transactional
    public CineResponseDTO crear(CineRequestDTO dto) {
        Cine cine = new Cine();
        copiarDatos(dto, cine);
        return DtoMapper.toCineResponse(cineRepository.save(cine));
    }

    @Transactional
    public CineResponseDTO actualizar(Long id, CineRequestDTO dto) {
        Cine cine = buscarEntidad(id);
        copiarDatos(dto, cine);
        return DtoMapper.toCineResponse(cineRepository.save(cine));
    }

    @Transactional
    public CineResponseDTO cambiarEstado(Long id, boolean activo) {
        Cine cine = buscarEntidad(id);
        cine.setActivo(activo);
        return DtoMapper.toCineResponse(cineRepository.save(cine));
    }

    @Transactional
    public void eliminar(Long id) {
        Cine cine = buscarEntidad(id);
        List<String> asociados = new ArrayList<>();
        if (salaRepository.existsByCineId(id)) {
            asociados.add("salas");
        }
        if (funcionRepository.existsBySalaCineId(id)) {
            asociados.add("funciones");
        }
        if (compraRepository.existsByFuncionSalaCineId(id)) {
            asociados.add("compras");
        }
        if (!asociados.isEmpty()) {
            throw new RecursoEnUsoException("El cine tiene " + String.join(", ", asociados) + " asociadas", asociados);
        }
        cineRepository.delete(cine);
    }

    @Transactional(readOnly = true)
    public Cine buscarEntidad(Long id) {
        return cineRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Cine no encontrado con id " + id));
    }

    private void copiarDatos(CineRequestDTO dto, Cine cine) {
        String nombre = Normalizador.formatoTitulo(dto.nombre());
        validarNombreUnico(nombre, cine.getId());
        cine.setNombre(nombre);
        cine.setProvincia(dto.provincia());
        cine.setLocalidad(normalizarLocalidad(dto.provincia(), dto.localidad()));
        cine.setCalle(Normalizador.texto(dto.calle()));
        cine.setAltura(dto.altura());
        cine.setTelefono(Normalizador.telefono(dto.telefono(), "telefono"));
        cine.setUrlImagen(Normalizador.texto(dto.urlImagen()));
    }

    private String normalizarLocalidad(Provincia provincia, String localidad) {
        if (provincia == Provincia.CABA) {
            return BarriosCaba.buscar(localidad)
                .orElseThrow(() -> new CampoInvalidoException("localidad", "Elegí un barrio de la lista"));
        }
        return Normalizador.formatoTitulo(localidad);
    }

    private void validarNombreUnico(String nombre, Long idPropio) {
        String clave = Normalizador.clave(nombre);
        boolean repetido = cineRepository.findAll().stream()
            .anyMatch(otro -> !Objects.equals(otro.getId(), idPropio) && Normalizador.clave(otro.getNombre()).equals(clave));
        if (repetido) {
            throw new CampoInvalidoException("nombre", "Ya existe un cine con ese nombre");
        }
    }
}
