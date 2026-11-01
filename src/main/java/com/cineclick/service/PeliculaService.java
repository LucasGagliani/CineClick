package com.cineclick.service;

import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.PeliculaDestacadaDTO;
import com.cineclick.dto.PeliculaRequestDTO;
import com.cineclick.dto.PeliculaResponseDTO;
import com.cineclick.exception.RecursoEnUsoException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.CategoriaDestacada;
import com.cineclick.model.EstadoFuncion;
import com.cineclick.model.Genero;
import com.cineclick.model.Pelicula;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.repository.ListaEsperaRepository;
import com.cineclick.repository.PeliculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PeliculaService {

    private static final int MAXIMO_DESTACADAS = 8;
    private static final int DIAS_NOVEDAD = 7;

    private final PeliculaRepository peliculaRepository;
    private final FuncionRepository funcionRepository;
    private final CompraRepository compraRepository;
    private final ListaEsperaRepository listaEsperaRepository;

    @Transactional(readOnly = true)
    public List<PeliculaResponseDTO> listar(Genero genero, boolean incluirInactivas) {
        if (incluirInactivas) {
            return peliculaRepository.findAll().stream()
                .sorted(Comparator.comparing(Pelicula::getTitulo, String.CASE_INSENSITIVE_ORDER))
                .map(DtoMapper::toPeliculaResponse)
                .toList();
        }
        List<Pelicula> peliculas = genero == null
            ? peliculaRepository.findByActivaTrue()
            : peliculaRepository.findByGeneroAndActivaTrue(genero);
        return peliculas.stream().map(DtoMapper::toPeliculaResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PeliculaDestacadaDTO> listarDestacadas() {
        return conCategoria().stream()
            .sorted(Comparator.comparing(PeliculaDestacadaDTO::categoria).thenComparingLong(destacada -> {
                long dia = destacada.pelicula().fechaEstreno().toEpochDay();
                return destacada.categoria() == CategoriaDestacada.NOVEDAD ? -dia : dia;
            }))
            .limit(MAXIMO_DESTACADAS)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PeliculaDestacadaDTO> listarProximosEstrenos() {
        return conCategoria().stream()
            .filter(destacada -> destacada.categoria() != CategoriaDestacada.NOVEDAD)
            .sorted(Comparator.comparing((PeliculaDestacadaDTO destacada) -> destacada.pelicula().fechaEstreno())
                .thenComparing(destacada -> destacada.pelicula().titulo()))
            .toList();
    }

    public CategoriaDestacada categoriaDe(Pelicula pelicula) {
        boolean aLaVenta = funcionRepository.existsByPeliculaIdAndEstadoAndFechaHoraInicioAfter(
            pelicula.getId(), EstadoFuncion.PROGRAMADA, LocalDateTime.now()
        );
        return categoria(pelicula, LocalDate.now(), aLaVenta);
    }

    private List<PeliculaDestacadaDTO> conCategoria() {
        LocalDate hoy = LocalDate.now();
        Set<Long> aLaVenta = funcionRepository.peliculasConFuncionesAVenta(LocalDateTime.now());
        return peliculaRepository.findByActivaTrue().stream()
            .map(pelicula -> {
                CategoriaDestacada categoria = categoria(pelicula, hoy, aLaVenta.contains(pelicula.getId()));
                return categoria == null ? null : new PeliculaDestacadaDTO(DtoMapper.toPeliculaResponse(pelicula), categoria);
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private static CategoriaDestacada categoria(Pelicula pelicula, LocalDate hoy, boolean aLaVenta) {
        LocalDate estreno = pelicula.getFechaEstreno();
        if (estreno == null) {
            return null;
        }
        if (estreno.isAfter(hoy)) {
            return aLaVenta ? CategoriaDestacada.PREVENTA : CategoriaDestacada.PROXIMAMENTE;
        }
        return estreno.isAfter(hoy.minusDays(DIAS_NOVEDAD)) ? CategoriaDestacada.NOVEDAD : null;
    }

    @Transactional(readOnly = true)
    public PeliculaResponseDTO buscarPorId(Long id) {
        return DtoMapper.toPeliculaResponse(buscarEntidad(id));
    }

    @Transactional
    public PeliculaResponseDTO crear(PeliculaRequestDTO dto) {
        Pelicula pelicula = new Pelicula();
        copiarDatos(dto, pelicula);
        validarDatos(pelicula);
        return DtoMapper.toPeliculaResponse(peliculaRepository.save(pelicula));
    }

    @Transactional
    public PeliculaResponseDTO actualizar(Long id, PeliculaRequestDTO dto) {
        Pelicula pelicula = buscarEntidad(id);
        copiarDatos(dto, pelicula);
        validarDatos(pelicula);
        return DtoMapper.toPeliculaResponse(peliculaRepository.save(pelicula));
    }

    @Transactional
    public PeliculaResponseDTO cambiarEstado(Long id, boolean activa) {
        Pelicula pelicula = buscarEntidad(id);
        pelicula.setActiva(activa);
        return DtoMapper.toPeliculaResponse(peliculaRepository.save(pelicula));
    }

    @Transactional
    public void eliminar(Long id) {
        Pelicula pelicula = buscarEntidad(id);
        List<String> asociados = new ArrayList<>();
        if (funcionRepository.existsByPeliculaId(id)) {
            asociados.add("funciones");
        }
        if (compraRepository.existsByFuncionPeliculaId(id)) {
            asociados.add("compras");
        }
        if (!asociados.isEmpty()) {
            throw new RecursoEnUsoException("La pelicula tiene " + String.join(", ", asociados) + " asociadas", asociados);
        }
        listaEsperaRepository.deleteByPeliculaId(id);
        peliculaRepository.delete(pelicula);
    }

    @Transactional(readOnly = true)
    public Pelicula buscarEntidad(Long id) {
        return peliculaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Pelicula no encontrada con id " + id));
    }

    public void validarDatos(Pelicula pelicula) {
        if (pelicula.getDuracionMinutos() == null || pelicula.getDuracionMinutos() <= 0) {
            throw new ReglaNegocioException("La duracion de la pelicula debe ser mayor a cero");
        }
        if (pelicula.getFormatosDisponibles() == null || pelicula.getFormatosDisponibles().isEmpty()) {
            throw new ReglaNegocioException("La pelicula debe tener al menos un formato disponible");
        }
    }

    private void copiarDatos(PeliculaRequestDTO dto, Pelicula pelicula) {
        pelicula.setTitulo(Normalizador.texto(dto.titulo()));
        pelicula.setSinopsis(Normalizador.texto(dto.sinopsis()));
        pelicula.setDuracionMinutos(dto.duracionMinutos());
        pelicula.setClasificacion(dto.clasificacion());
        pelicula.setGenero(dto.genero());
        pelicula.setIdioma(dto.idioma());
        pelicula.setFormatosDisponibles(dto.formatosDisponibles());
        pelicula.setUrlPoster(Normalizador.texto(dto.urlPoster()));
        pelicula.setFechaEstreno(dto.fechaEstreno());
        if (dto.destacada() != null) {
            pelicula.setDestacada(dto.destacada());
        }
    }
}
