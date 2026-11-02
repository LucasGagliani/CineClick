package com.cineclick.controller;

import com.cineclick.dto.EstadoRequestDTO;
import com.cineclick.dto.PromocionRequestDTO;
import com.cineclick.dto.PromocionResponseDTO;
import com.cineclick.service.PromocionService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/promociones")
@RequiredArgsConstructor
public class PromocionController {

    private final PromocionService promocionService;

    @GetMapping
    public List<PromocionResponseDTO> listar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return promocionService.listarTodas(usuarioId);
    }

    @GetMapping("/vigentes")
    public List<PromocionResponseDTO> listarVigentes(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return promocionService.listarVigentes(usuarioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromocionResponseDTO crear(@Valid @RequestBody PromocionRequestDTO dto) {
        return promocionService.crear(dto);
    }

    @PutMapping("/{id}")
    public PromocionResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody PromocionRequestDTO dto) {
        return promocionService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/estado")
    public PromocionResponseDTO cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequestDTO dto) {
        return promocionService.cambiarEstado(id, dto.activo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        promocionService.eliminar(id);
    }
}
