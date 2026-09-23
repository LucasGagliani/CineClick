package com.cineclick.service;

import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.PeliculaRequestDTO;
import com.cineclick.dto.PeliculaResponseDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Genero;
import com.cineclick.model.Pelicula;
import com.cineclick.repository.PeliculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PeliculaService {

    private final PeliculaRepository peliculaRepository;

    @Transactional(readOnly = true)
    public List<PeliculaResponseDTO> listar(Genero genero) {
        List<Pelicula> peliculas = genero == null
            ? peliculaRepository.findByActivaTrue()
            : peliculaRepository.findByGeneroAndActivaTrue(genero);
        return peliculas.stream().map(DtoMapper::toPeliculaResponse).toList();
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
    public void eliminar(Long id) {
        Pelicula pelicula = buscarEntidad(id);
        pelicula.setActiva(false);
        peliculaRepository.save(pelicula);
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
        pelicula.setTitulo(dto.titulo());
        pelicula.setSinopsis(dto.sinopsis());
        pelicula.setDuracionMinutos(dto.duracionMinutos());
        pelicula.setClasificacion(dto.clasificacion());
        pelicula.setGenero(dto.genero());
        pelicula.setIdioma(dto.idioma());
        pelicula.setFormatosDisponibles(dto.formatosDisponibles());
        pelicula.setUrlPoster(dto.urlPoster());
        pelicula.setFechaEstreno(dto.fechaEstreno());
    }
}
