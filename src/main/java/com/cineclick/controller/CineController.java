package com.cineclick.controller;

import com.cineclick.config.SoloAdmin;
import com.cineclick.dto.CineRequestDTO;
import com.cineclick.dto.CineResponseDTO;
import com.cineclick.dto.EstadoRequestDTO;
import com.cineclick.dto.SalaRequestDTO;
import com.cineclick.dto.SalaResponseDTO;
import com.cineclick.service.CineService;
import com.cineclick.service.SalaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/cines")
@RequiredArgsConstructor
public class CineController {

    private final CineService cineService;
    private final SalaService salaService;

    @GetMapping
    public List<CineResponseDTO> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return cineService.listar(incluirInactivos);
    }

    @GetMapping("/{id}")
    public CineResponseDTO obtener(@PathVariable Long id) {
        return cineService.buscarPorId(id);
    }

    @SoloAdmin
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CineResponseDTO crear(@Valid @RequestBody CineRequestDTO dto) {
        return cineService.crear(dto);
    }

    @SoloAdmin
    @PutMapping("/{id}")
    public CineResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody CineRequestDTO dto) {
        return cineService.actualizar(id, dto);
    }

    @SoloAdmin
    @PatchMapping("/{id}/estado")
    public CineResponseDTO cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequestDTO dto) {
        return cineService.cambiarEstado(id, dto.activo());
    }

    @SoloAdmin
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        cineService.eliminar(id);
    }

    @GetMapping("/{id}/salas")
    public List<SalaResponseDTO> listarSalas(@PathVariable Long id) {
        return salaService.listarDelCine(id);
    }

    @SoloAdmin
    @PostMapping("/{id}/salas")
    @ResponseStatus(HttpStatus.CREATED)
    public SalaResponseDTO agregarSala(@PathVariable Long id, @Valid @RequestBody SalaRequestDTO dto) {
        return salaService.crear(id, dto);
    }
}
