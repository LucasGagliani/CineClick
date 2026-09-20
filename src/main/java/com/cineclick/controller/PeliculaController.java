package com.cineclick.controller;

import com.cineclick.dto.PeliculaRequestDTO;
import com.cineclick.dto.PeliculaResponseDTO;
import com.cineclick.model.Genero;
import com.cineclick.service.PeliculaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/peliculas")
public class PeliculaController {

    private final PeliculaService peliculaService;

    public PeliculaController(PeliculaService peliculaService) {
        this.peliculaService = peliculaService;
    }

    @GetMapping
    public List<PeliculaResponseDTO> listar(@RequestParam(required = false) Genero genero) {
        return peliculaService.listar(genero);
    }

    @GetMapping("/{id}")
    public PeliculaResponseDTO obtener(@PathVariable Long id) {
        return peliculaService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PeliculaResponseDTO crear(@Valid @RequestBody PeliculaRequestDTO dto) {
        return peliculaService.crear(dto);
    }

    @PutMapping("/{id}")
    public PeliculaResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody PeliculaRequestDTO dto) {
        return peliculaService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        peliculaService.eliminar(id);
    }
}
