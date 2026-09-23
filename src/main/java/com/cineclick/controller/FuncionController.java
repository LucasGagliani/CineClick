package com.cineclick.controller;

import com.cineclick.dto.ButacaResponseDTO;
import com.cineclick.dto.FuncionRequestDTO;
import com.cineclick.dto.FuncionResponseDTO;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.service.FuncionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/funciones")
@RequiredArgsConstructor
public class FuncionController {

    private final FuncionService funcionService;

    @GetMapping
    public List<FuncionResponseDTO> listar(
        @RequestParam(required = false) Long peliculaId,
        @RequestParam(required = false) Long cineId,
        @RequestParam(required = false) String ciudad,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        @RequestParam(required = false) FormatoFuncion formato
    ) {
        return funcionService.listar(peliculaId, cineId, ciudad, fecha, formato);
    }

    @GetMapping("/{id}")
    public FuncionResponseDTO obtener(@PathVariable Long id) {
        return funcionService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FuncionResponseDTO crear(@Valid @RequestBody FuncionRequestDTO dto) {
        return funcionService.crear(dto);
    }

    @PutMapping("/{id}")
    public FuncionResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody FuncionRequestDTO dto) {
        return funcionService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        funcionService.eliminar(id);
    }

    @GetMapping("/{id}/butacas-disponibles")
    public List<ButacaResponseDTO> obtenerButacasDisponibles(@PathVariable Long id) {
        return funcionService.obtenerButacasDisponibles(id);
    }
}
