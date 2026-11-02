package com.cineclick.controller;

import com.cineclick.config.SoloAdmin;
import com.cineclick.dto.EstadoRequestDTO;
import com.cineclick.dto.SalaRequestDTO;
import com.cineclick.dto.SalaResponseDTO;
import com.cineclick.service.SalaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/salas")
@RequiredArgsConstructor
public class SalaController {

    private final SalaService salaService;

    @GetMapping
    public List<SalaResponseDTO> listar() {
        return salaService.listarTodas();
    }

    @SoloAdmin
    @PutMapping("/{id}")
    public SalaResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody SalaRequestDTO dto) {
        return salaService.actualizar(id, dto);
    }

    @SoloAdmin
    @PatchMapping("/{id}/estado")
    public SalaResponseDTO cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequestDTO dto) {
        return salaService.cambiarEstado(id, dto.activo());
    }

    @SoloAdmin
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        salaService.eliminar(id);
    }
}
