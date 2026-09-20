package com.cineclick.service;

import com.cineclick.dto.CineRequestDTO;
import com.cineclick.dto.CineResponseDTO;
import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.SalaRequestDTO;
import com.cineclick.dto.SalaResponseDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Butaca;
import com.cineclick.model.Cine;
import com.cineclick.model.Sala;
import com.cineclick.model.TipoButaca;
import com.cineclick.repository.CineRepository;
import com.cineclick.repository.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CineService {

    private final CineRepository cineRepository;
    private final SalaRepository salaRepository;

    public CineService(CineRepository cineRepository, SalaRepository salaRepository) {
        this.cineRepository = cineRepository;
        this.salaRepository = salaRepository;
    }

    @Transactional(readOnly = true)
    public List<CineResponseDTO> listar(String ciudad) {
        List<Cine> cines = ciudad == null || ciudad.isBlank()
            ? cineRepository.findByActivoTrue()
            : cineRepository.findByCiudadIgnoreCaseAndActivoTrue(ciudad);
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
    public void eliminar(Long id) {
        Cine cine = buscarEntidad(id);
        cine.setActivo(false);
        cineRepository.save(cine);
    }

    @Transactional
    public SalaResponseDTO agregarSala(Long cineId, SalaRequestDTO dto) {
        if (dto.cantidadFilas() > 26) {
            throw new ReglaNegocioException("La cantidad de filas no puede superar 26");
        }
        Cine cine = buscarEntidad(cineId);
        Sala sala = new Sala();
        sala.setNombre(dto.nombre());
        sala.setNumero(dto.numero());
        sala.setTipo(dto.tipo());
        for (int fila = 0; fila < dto.cantidadFilas(); fila++) {
            for (int numero = 1; numero <= dto.butacasPorFila(); numero++) {
                Butaca butaca = new Butaca();
                butaca.setFila(String.valueOf((char) ('A' + fila)));
                butaca.setNumero(numero);
                butaca.setTipo(numero <= 2 ? TipoButaca.VIP : TipoButaca.NORMAL);
                sala.agregarButaca(butaca);
            }
        }
        cine.agregarSala(sala);
        cineRepository.save(cine);
        return DtoMapper.toSalaResponse(sala);
    }

    @Transactional(readOnly = true)
    public List<SalaResponseDTO> listarSalas(Long cineId) {
        buscarEntidad(cineId);
        return salaRepository.findByCineIdAndActivaTrue(cineId).stream()
            .map(DtoMapper::toSalaResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Cine buscarEntidad(Long id) {
        return cineRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Cine no encontrado con id " + id));
    }

    private void copiarDatos(CineRequestDTO dto, Cine cine) {
        cine.setNombre(dto.nombre());
        cine.setDireccion(dto.direccion());
        cine.setCiudad(dto.ciudad());
        cine.setTelefono(dto.telefono());
    }
}
