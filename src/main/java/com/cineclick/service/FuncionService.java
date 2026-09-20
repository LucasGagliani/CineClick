package com.cineclick.service;

import com.cineclick.dto.ButacaResponseDTO;
import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.FuncionRequestDTO;
import com.cineclick.dto.FuncionResponseDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Butaca;
import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoEntrada;
import com.cineclick.model.EstadoFuncion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Funcion;
import com.cineclick.model.Pelicula;
import com.cineclick.model.Sala;
import com.cineclick.repository.ButacaRepository;
import com.cineclick.repository.EntradaRepository;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.repository.PeliculaRepository;
import com.cineclick.repository.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FuncionService {

    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final ButacaRepository butacaRepository;
    private final EntradaRepository entradaRepository;

    public FuncionService(
        FuncionRepository funcionRepository,
        PeliculaRepository peliculaRepository,
        SalaRepository salaRepository,
        ButacaRepository butacaRepository,
        EntradaRepository entradaRepository
    ) {
        this.funcionRepository = funcionRepository;
        this.peliculaRepository = peliculaRepository;
        this.salaRepository = salaRepository;
        this.butacaRepository = butacaRepository;
        this.entradaRepository = entradaRepository;
    }

    @Transactional(readOnly = true)
    public List<FuncionResponseDTO> listar(Long peliculaId, Long cineId, String ciudad, LocalDate fecha, FormatoFuncion formato) {
        return funcionRepository.findAll().stream()
            .filter(funcion -> peliculaId == null || funcion.getPelicula().getId().equals(peliculaId))
            .filter(funcion -> cineId == null || funcion.getSala().getCine().getId().equals(cineId))
            .filter(funcion -> ciudad == null || ciudad.isBlank() || funcion.getSala().getCine().getCiudad().equalsIgnoreCase(ciudad))
            .filter(funcion -> fecha == null || funcion.getFechaHoraInicio().toLocalDate().equals(fecha))
            .filter(funcion -> formato == null || funcion.getFormato() == formato)
            .map(DtoMapper::toFuncionResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public FuncionResponseDTO buscarPorId(Long id) {
        return DtoMapper.toFuncionResponse(buscarEntidad(id));
    }

    @Transactional
    public FuncionResponseDTO crear(FuncionRequestDTO dto) {
        Funcion funcion = new Funcion();
        copiarDatos(dto, funcion);
        validarHorario(funcion, null);
        return DtoMapper.toFuncionResponse(funcionRepository.save(funcion));
    }

    @Transactional
    public FuncionResponseDTO actualizar(Long id, FuncionRequestDTO dto) {
        Funcion funcion = buscarEntidad(id);
        copiarDatos(dto, funcion);
        validarHorario(funcion, id);
        return DtoMapper.toFuncionResponse(funcionRepository.save(funcion));
    }

    @Transactional
    public void eliminar(Long id) {
        Funcion funcion = buscarEntidad(id);
        funcion.cambiarEstado(EstadoFuncion.CANCELADA);
        funcionRepository.save(funcion);
    }

    @Transactional(readOnly = true)
    public List<ButacaResponseDTO> obtenerButacasDisponibles(Long funcionId) {
        Funcion funcion = buscarEntidad(funcionId);
        Set<Long> butacasOcupadas = entradaRepository.findByFuncionId(funcionId).stream()
            .filter(entrada -> entrada.getEstado() != EstadoEntrada.CANCELADA)
            .map(entrada -> entrada.getButaca().getId())
            .collect(Collectors.toSet());
        return butacaRepository.findBySalaIdAndActivaTrue(funcion.getSala().getId()).stream()
            .map(butaca -> DtoMapper.toButacaResponse(butaca, !butacasOcupadas.contains(butaca.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public Funcion buscarEntidad(Long id) {
        return funcionRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Funcion no encontrada con id " + id));
    }

    public void validarHorario(Funcion funcion, Long idIgnorado) {
        if (!funcion.getPelicula().getFormatosDisponibles().contains(funcion.getFormato())) {
            throw new ReglaNegocioException("La pelicula no esta disponible en el formato solicitado");
        }
        funcion.calcularHorarioFin();
        if (funcion.getFechaHoraInicio().isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("La funcion debe programarse a futuro");
        }
        boolean seSolapa = funcionRepository.findBySalaId(funcion.getSala().getId()).stream()
            .filter(existente -> idIgnorado == null || !existente.getId().equals(idIgnorado))
            .filter(existente -> existente.getEstado() != EstadoFuncion.CANCELADA)
            .anyMatch(existente -> funcion.getFechaHoraInicio().isBefore(existente.getFechaHoraFin())
                && funcion.getFechaHoraFin().isAfter(existente.getFechaHoraInicio()));
        if (seSolapa) {
            throw new ReglaNegocioException("La sala ya tiene una funcion programada en ese horario");
        }
    }

    private void copiarDatos(FuncionRequestDTO dto, Funcion funcion) {
        Pelicula pelicula = peliculaRepository.findById(dto.peliculaId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Pelicula no encontrada con id " + dto.peliculaId()));
        Sala sala = salaRepository.findById(dto.salaId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Sala no encontrada con id " + dto.salaId()));
        funcion.setPelicula(pelicula);
        funcion.setSala(sala);
        funcion.setFechaHoraInicio(dto.fechaHoraInicio());
        funcion.setPrecioBase(dto.precioBase());
        funcion.setFormato(dto.formato());
        funcion.setIdioma(dto.idioma());
    }
}
